/*
 * Copyright 2018 HM Revenue & Customs
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

import org.joda.time.DateTime
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.rasapi.config.AppContext
import uk.gov.hmrc.rasapi.connectors.{DesConnector, FileUploadConnector}
import uk.gov.hmrc.rasapi.helpers.ResidencyYearResolver
import uk.gov.hmrc.rasapi.metrics.Metrics
import uk.gov.hmrc.rasapi.models.{CallbackData, ResultsFileMetaData}
import uk.gov.hmrc.rasapi.repository.RasRepository
import play.api.mvc.{AnyContent, Request}
import java.nio.file.Path

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object FileProcessingService extends FileProcessingService {

  override val fileUploadConnector: FileUploadConnector = FileUploadConnector
  override val desConnector: DesConnector = DesConnector
  override val residencyYearResolver: ResidencyYearResolver = ResidencyYearResolver
  override val auditService: AuditService = AuditService
  override def getCurrentDate: DateTime = DateTime.now()
  override val allowDefaultRUK: Boolean = AppContext.allowDefaultRUK
  override val retryLimit: Int = AppContext.requestRetryLimit
  override val waitTime: Long = AppContext.waitTimeBeforeRetryingRequest
}

trait FileProcessingService extends RasFileReader with RasFileWriter with ResultsGenerator with SessionCacheService {
  val fileProcess = "File-Processing"
  val fileRead = "File-Upload-Read"
  val fileResults = "File-Results"
  val fileSave = "File-Save"
  val STATUS_ERROR = "ERROR"

  def processFile(userId: String, callbackData: CallbackData)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Unit = {
    val fileMetrics = Metrics.register(fileProcess).time
    val fileReadMetrics = Metrics.register(fileRead).time

    readFile(callbackData.envelopeId, callbackData.fileId).onComplete {
      fileReadMetrics.stop
      inputFileData =>
        if(inputFileData.isSuccess)
        {
          manipulateFile(inputFileData, userId, callbackData, SessionCacheService)
        }
    }

    fileMetrics.stop
  }

  def manipulateFile(inputFileData: Try[Iterator[String]], userId: String, callbackData: CallbackData, sessionCacheService: SessionCacheService)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Unit = {
    val fileResultsMetrics = Metrics.register(fileResults).time
    val writer = createFileWriter(callbackData.fileId)
    try{
      val dataIterator = inputFileData.get.toList
      Logger.warn("file data size " + dataIterator.size + " of user " + userId)
      dataIterator.foreach(row => if (!row.isEmpty) writeResultToFile(writer._2,fetchResult(row,userId)) )
      closeWriter(writer._2)
      fileResultsMetrics.stop

      saveFile(writer._1, userId, callbackData)

    } catch
      {
        case ex:Throwable => {
          Logger.error("error in File processing -> " + ex.getMessage)
          sessionCacheService.updateFileSession(userId, callbackData.copy(status = STATUS_ERROR), None)
          fileResultsMetrics.stop
        }
      }
    finally {
      closeWriter(writer._2)
      clearFile( writer._1)
    }
  }

  def saveFile(filePath: Path, userId: String, callbackData: CallbackData)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Unit = {
    val fileSaveMetrics = Metrics.register(fileSave).time
    RasRepository.filerepo.saveFile(userId, callbackData.envelopeId, filePath, callbackData.fileId).onComplete {
      result =>
        clearFile(filePath)
        result match {
          case Success(file) => SessionCacheService.updateFileSession(userId, callbackData,
            Some(ResultsFileMetaData(file.id.toString, file.filename, file.uploadDate, file.chunkSize, file.length)))

          case Failure(ex) => {
            Logger.error("results file generation/saving failed with Exception " + ex.getMessage)
            SessionCacheService.updateFileSession(userId, callbackData.copy(status = STATUS_ERROR), None)
          }
          //delete result  a future ind
        }
        fileSaveMetrics.stop
        fileUploadConnector.deleteUploadedFile(callbackData.envelopeId, callbackData.fileId)
    }
  }
}


