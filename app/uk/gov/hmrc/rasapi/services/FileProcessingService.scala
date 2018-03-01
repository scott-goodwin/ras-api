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
import uk.gov.hmrc.rasapi.models.{CallbackData, ResultsFileMetaData}
import uk.gov.hmrc.rasapi.repository.RasRepository
import play.api.mvc.{AnyContent, Request}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object FileProcessingService extends FileProcessingService {

  override val fileUploadConnector: FileUploadConnector = FileUploadConnector
  override val desConnector: DesConnector = DesConnector
  override val residencyYearResolver: ResidencyYearResolver = ResidencyYearResolver
  override val auditService: AuditService = AuditService
  override def getCurrentDate: DateTime = DateTime.now()
  override val allowDefaultRUK: Boolean = AppContext.allowDefaultRUK
}

trait FileProcessingService extends RasFileReader with RasFileWriter with ResultsGenerator with SessionCacheService {

  def processFile(userId: String, callbackData: CallbackData)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Unit = {

    readFile(callbackData.envelopeId, callbackData.fileId).onComplete {

      inputFileData => if(inputFileData.isSuccess)
        {
          val writer = createFileWriter(callbackData.fileId)
          try{
            inputFileData.get.foreach(row => if (!row.isEmpty) writeResultToFile(writer._2,fetchResult(row,userId)) )
            closeWriter(writer._2)
            RasRepository.filerepo.saveFile(userId, callbackData.envelopeId, writer._1, callbackData.fileId).onComplete {
              result =>
                clearFile( writer._1)
                result match {
                  case Success(file) =>
                    SessionCacheService.updateFileSession(userId, callbackData,
                    Some(ResultsFileMetaData(file.id.toString, file.filename, file.uploadDate, file.chunkSize, file.length)))

                  case Failure(ex) =>
                    Logger.error("results file generation/saving failed with Exception " + ex.getMessage)
                  //delete result  a future ind
                }
                fileUploadConnector.deleteUploadedFile(callbackData.envelopeId, callbackData.fileId)
            }
          } catch
            {
              case ex:Throwable =>
                ex.printStackTrace()
                Logger.error("error in File processing -> " + ex.getMessage )
                clearFile( writer._1)
            }
        }

    }
  }
}


