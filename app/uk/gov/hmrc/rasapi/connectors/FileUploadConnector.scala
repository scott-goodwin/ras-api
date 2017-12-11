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
import java.nio.file.Path

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Source, StreamConverters}
import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.{WS, WSRequest}
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
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
        None
      }
    }
  }

  def uploadFile(envelopeId: String, fileId: String, filePath:Path) = {

        val  url = s"$serviceUrl/$fileUploadFEBaseUrl/${envelopeId}/files/${2}"

         WS.url(url).post(Source(FilePart("results", "results.csv", Option("text/plain"), FileIO.fromPath(filePath)) :: DataPart("rasFileKey", "value") :: List()))

      }


    //another way to stream data : not sure how to do
/*      val res =  StreamConverters.asOutputStream().mapMaterializedValue { outputStream =>
        // write stuff to outputStream
      }
      // The sink that writes to the output stream
      val sink = Sink.foreach[ByteString] { bytes =>
        outputStream.write(bytes.toArray)
      }*/

//      ws.url(url).post(Source(FilePart("hello", "hello.txt", Option("text/plain"), FileIO.fromPath(tmpFile.toPath)) :: DataPart("key", "value") :: List()))



  private def streamResponse(request: WSRequest) = Source.fromFuture(request.stream()).flatMapConcat(_.body)


}

object FileUploadConnector extends FileUploadConnector {

  override val http: HttpPost = WSHttp
  override val wsHttp: WSHttp = WSHttp
}




/*
class FileUpload {
  import org.apache.http.entity.mime._
  import java.io.File

  import org.apache.http.entity.mime.content._
  import java.io.ByteArrayOutputStream
  import play.api.libs.ws.WS



  val contents ="contents string"
  val file = File.createTempFile("sample", ".txt")

 def upload: Unit = {
   val bw = new java.io.BufferedWriter(new java.io.FileWriter(file))
     bw.write("");
   bw.close();

   builder.addPart("file", new FileBody(file, org.apache.http.entity.ContentType.create("text/plain"), "sample"))
   builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
   val entity = builder.build

   val outputstream = new ByteArrayOutputStream
   entity.writeTo(outputstream)
   val header = (entity.getContentType.getName -> entity.getContentType.getValue)
   val response = WS.url("/post/file").withHeaders(header).post(outputstream.toByteArray())
 }

}
*/
