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

package uk.gov.hmrc.rasapi.connectors

import java.io.{BufferedReader, InputStreamReader}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.ws.{DefaultWSResponseHeaders, StreamedResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.config.WSHttp
import uk.gov.hmrc.rasapi.services.RASWsHelpers

import scala.concurrent.Future

class FileUploadConnectorSpec extends UnitSpec with RASWsHelpers with OneAppPerSuite with MockitoSugar with ServicesConfig with WSHttp{

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockWsHttp = mock[WSHttp]

  object TestConnector extends FileUploadConnector {
    override val http: HttpPost = mock[HttpPost]
    override val wsHttp: WSHttp = mockWsHttp
  }

  val envelopeId: String = "0b215e97-11d4-4006-91db-c067e74fc653"
  val fileId: String = "file-id-1"


  "getFile" should {

    "return an StreamedResponse from File-Upload service" in {

      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()

      val streamResponse:StreamedResponse = StreamedResponse(DefaultWSResponseHeaders(200, Map("CONTENT_TYPE" -> Seq("application/octet-stream"))),
        Source.apply[ByteString](Seq(ByteString("Test"),  ByteString("\r\n"), ByteString("Passed")).to[scala.collection.immutable.Iterable]) )

      when(mockWsHttp.buildRequestWithStream(any())(any())).thenReturn(Future.successful(streamResponse))

      val values = List("Test", "Passed")

      val result = await(TestConnector.getFile(envelopeId, fileId))

      val reader = new BufferedReader(new InputStreamReader(result.get))

      (Iterator continually reader.readLine takeWhile (_ != null) toList) should contain theSameElementsAs List("Test", "Passed")

    }
  }

  "deleteUploadedFile" should {
    "submit delete request to file-upload service" in {
      when(mockWsHttp.doDelete(any())(any())).thenReturn(Future.successful(HttpResponse(200)))
      val result = await(TestConnector.deleteUploadedFile(envelopeId, fileId))
      result shouldBe true
    }
    "failed delete request to file-upload service" in {
      when(mockWsHttp.doDelete(any())(any())).thenReturn(Future.successful(HttpResponse(400)))
      val result = await(TestConnector.deleteUploadedFile(envelopeId, fileId))
      result shouldBe false
    }
  }
}
