/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.rasapi.services

import java.nio.file.Path

import javax.inject.Inject
import org.joda.time.DateTime
import play.api.Logger
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.rasapi.config.AppContext
import uk.gov.hmrc.rasapi.connectors.{DesConnector, FileUploadConnector}
import uk.gov.hmrc.rasapi.helpers.ResidencyYearResolver
import uk.gov.hmrc.rasapi.metrics.Metrics
import uk.gov.hmrc.rasapi.models.{ApiVersion, CallbackData, ResultsFileMetaData}
import uk.gov.hmrc.rasapi.repository.RasFilesRepository

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class FileProcessingService @Inject()(
                                     val fileUploadConnector: FileUploadConnector,
                                     val desConnector: DesConnector,
                                     val residencyYearResolver: ResidencyYearResolver,
                                     val auditService: AuditService,
                                     val sessionCacheService: SessionCacheService,
                                     val fileRepo: RasFilesRepository,
                                     val appContext: AppContext,
                                     val metrics: Metrics,
                                     implicit val ec: ExecutionContext
                                     ) extends RasFileReader with RasFileWriter with ResultsGenerator {

  def getCurrentDate: DateTime = DateTime.now()

  val allowDefaultRUK: Boolean = appContext.allowDefaultRUK
  val DECEASED: String = appContext.deceasedStatus
  val MATCHING_FAILED: String = appContext.matchingFailedStatus
  val INTERNAL_SERVER_ERROR: String = appContext.internalServerErrorStatus
  val SERVICE_UNAVAILABLE: String = appContext.serviceUnavailableStatus
  val FILE_PROCESSING_MATCHING_FAILED: String = appContext.fileProcessingMatchingFailedStatus
  val FILE_PROCESSING_INTERNAL_SERVER_ERROR: String = appContext.fileProcessingInternalServerErrorStatus

  val fileProcess = "File-Processing"
  val fileRead = "File-Upload-Read"
  val fileResults = "File-Results"
  val fileSave = "File-Save"
  val STATUS_ERROR = "ERROR"

  def processFile(userId: String, callbackData: CallbackData, apiVersion: ApiVersion)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Unit = {
    val fileMetrics = metrics.register(fileProcess).time
    val fileReadMetrics = metrics.register(fileRead).time

    readFile(callbackData.envelopeId, callbackData.fileId, userId).onComplete {
      fileReadMetrics.stop
      inputFileData =>
        if(inputFileData.isSuccess)
        {
          manipulateFile(inputFileData, userId, callbackData, apiVersion)
        }
    }

    fileMetrics.stop
  }

  def manipulateFile(inputFileData: Try[Iterator[String]], userId: String, callbackData: CallbackData, apiVersion: ApiVersion)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Unit = {
    val fileResultsMetrics = metrics.register(fileResults).time
    val writer = createFileWriter(callbackData.fileId, userId)

    def removeDoubleQuotes(row: String): String = {
      row match {
        case s if s.startsWith("\"") && s.endsWith("\"") => s.drop(1).dropRight(1)
        case _ => row
      }
    }

    try{
      val dataIterator = inputFileData.get.toList
      Logger.warn(s"file data size ${dataIterator.size} for user $userId")
      writeResultToFile(writer._2, s"National Insurance number,First name,Last name,Date of birth,$getTaxYearHeadings", userId)
      dataIterator.foreach(row =>
        if (!row.isEmpty) {
          writeResultToFile(writer._2,fetchResult(removeDoubleQuotes(row),userId,callbackData.fileId, apiVersion = apiVersion), userId)
        }
      )
      closeWriter(writer._2)
      fileResultsMetrics.stop

      Logger.warn(s"File results complete, ready to save the file (fileId: ${callbackData.fileId}) for userId ($userId).")

      saveFile(writer._1, userId, callbackData)

    } catch
      {
        case ex:Throwable => {
          Logger.error(s"error for userId ($userId) in File processing -> ${ex.getMessage}")
          sessionCacheService.updateFileSession(userId, callbackData.copy(status = STATUS_ERROR), None, None)
          fileResultsMetrics.stop
        }
      }
    finally {
      closeWriter(writer._2)
      clearFile( writer._1, userId)
    }
  }

  def saveFile(filePath: Path, userId: String, callbackData: CallbackData)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Unit = {

    Logger.warn("[FileProcessingService] Starting to save file...")

    val fileSaveMetrics = metrics.register(fileSave).time
    try {
      fileRepo.saveFile(userId, callbackData.envelopeId, filePath, callbackData.fileId).onComplete {
        result =>

          result match {
            case Success(file) =>
              Logger.warn(s"Starting to save the file (${file.id}) for user ID: $userId")

              val resultsFileMetaData = Some(ResultsFileMetaData(file.id.toString, file.filename, file.uploadDate, file.chunkSize, file.length))

              fileUploadConnector.getFileMetadata(callbackData.envelopeId, callbackData.fileId, userId).onComplete {
                case Success(metadata) =>
                  sessionCacheService.updateFileSession(userId, callbackData, resultsFileMetaData, metadata)
                case Failure(ex) =>
                  Logger.error(s"Failed to get File Metadata for file (${file.id}), for user ID: $userId.", ex)
                  sessionCacheService.updateFileSession(userId, callbackData, resultsFileMetaData, None)
              }

              Logger.warn(s"Completed saving the file (${file.id}) for user ID: $userId")

            case Failure(ex) => {
              Logger.error(s"results file for userId ($userId) generation/saving failed with Exception ${ex.getMessage}")
              sessionCacheService.updateFileSession(userId, callbackData.copy(status = STATUS_ERROR), None, None)
            }
            //delete result  a future ind
          }
          fileSaveMetrics.stop
          fileUploadConnector.deleteUploadedFile(callbackData.envelopeId, callbackData.fileId, userId)
      }
    }
  }

  def getTaxYearHeadings: String = {
    val currentDate = getCurrentDate
    val currentYear = currentDate.getYear
    if (currentDate.isAfter(new DateTime(currentYear - 1, 12, 31, 0, 0, 0, 0)) && currentDate.isBefore(new DateTime(currentYear, 4, 6, 0, 0, 0, 0)))
      s"${currentYear - 1} to $currentYear residency status,$currentYear to ${currentYear + 1} residency status"
    else
      s"$currentYear to ${currentYear + 1} residency status"
  }
}


