/*
 * Copyright 2017 HM Revenue & Customs
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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.rasapi.connectors.{DesConnector, FileUploadConnector}
import uk.gov.hmrc.rasapi.repository.RasRepository

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

object FileProcessingService extends FileProcessingService {

  override val fileUploadConnector: FileUploadConnector = FileUploadConnector
  override val desConnector:DesConnector = DesConnector
}

trait FileProcessingService extends RasFileReader with RasFileWriter with ResultsGenerator{


  def processFile(envelopeId: String, fileId: String)(implicit hc: HeaderCarrier)  = {

    lazy val results:ListBuffer[String] = ListBuffer.empty

    createResultsFile(readFile(envelopeId,fileId).map { res =>
      res.map( row => if (!row.isEmpty) {fetchResult(row).map(results += _)})
      }).onComplete{
      case res =>  RasRepository.filerepo.saveFile(res.get).map{file=> clearFile(res.get)
        //delete file a future ind
        fileUploadConnector.deleteUploadedFile(envelopeId,fileId)
        //update status as success for the envelope in session-cache to confirm it is processed
        //if exception mark status as error and save into session
       }
    }
    }
  }


