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

package uk.gov.hmrc.rasapi.connectors

import java.io.InputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters
import play.api.Logger
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.rasapi.config.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait FileUploadConnector extends ServicesConfig {

  val http: HttpPost
  val wsHttp: WSHttp

  lazy val serviceUrl = baseUrl("file-upload")
  lazy val fileUploadFEBaseUrl = baseUrl("file-upload-frontend")
  lazy val fileUploadUrlSuffix = getString("file-upload-url-suffix")


  def getFile(envelopeId: String, fileId: String)(implicit hc: HeaderCarrier): Future[Option[InputStream]] = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    Logger.debug(s"Get to file-upload with URI : /file-upload/envelopes/${envelopeId}/files/${fileId}/content")
    wsHttp.buildRequestWithStream(s"$serviceUrl/$fileUploadUrlSuffix/${envelopeId}/files/${fileId}/content").map { res =>
      Some(res.body.runWith(StreamConverters.asInputStream()))
    } recover {
      case ex: Throwable => {
        Logger.error("Exception thrown while retrieving file / converting to InputStream.", ex)
        throw new RuntimeException("Error Streaming file from file-upload service")
      }
    }
  }

  def deleteUploadedFile(envelopeId: String, fileId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {

    wsHttp.doDelete(s"$serviceUrl/$fileUploadUrlSuffix/${envelopeId}/files/${fileId}").map { res =>
       if(res.status == 200) {Logger.warn("user file deleted successfully from file-upload"); true}
       else {Logger.error(s"Failed to delete user file => envelopeID: ${envelopeId}/files/${fileId}" )
       false}
    }.recover{
      case _ => Logger.error(s"failed to execute delete user file => envelopeID : ${envelopeId}/files/${fileId}")
        false
    }
  }
}

object FileUploadConnector extends FileUploadConnector {

  override val http: HttpPost = WSHttp
  override val wsHttp: WSHttp = WSHttp
}




