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

import java.util.UUID

import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatest.{WordSpec, _}
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}
import uk.gov.hmrc.rasapi.models.{CustomerDetails, Nino, ResidencyStatus}

import scala.concurrent.Future

class CachingConnectorSpec extends WordSpec with MockitoSugar with ShouldMatchers {

  implicit val hc = HeaderCarrier()

  val mockHttp = mock[HttpPost]

  object TestCachingConnector extends CachingConnector {
    override val http: HttpPost = mockHttp
  }

  val nino = Json.parse("""{"nino" : "AB123456C"}""".stripMargin

  )


  "getCachedData" should {
    "return a nino when a valid uuid is provided" in {

      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val uuid = UUID.randomUUID.toString

      when(mockHttp.POST[HttpResponse, HttpResponse](Matchers.any(),Matchers.any(), Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(nino))))

      val result = TestCachingConnector.getCachedData(Matchers.eq(uuid))

      await(result) shouldBe Nino("AB123456C")

    }
}
