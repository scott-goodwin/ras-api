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
import org.scalatest.{ShouldMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}
import uk.gov.hmrc.rasapi.models.{CustomerDetails, ResidencyStatus}
import uk.gov.hmrc.rasapi.models._

import scala.concurrent.Future

class DESConnectorSpec extends WordSpec with OneAppPerSuite with MockitoSugar with ShouldMatchers{

  val mockHttp = mock[HttpPost]

  object TestDesConnector extends DesConnector {
    override val http: HttpPost = mockHttp
  }

  val residencyStatus = Json.parse(
    """{
         "currentYearResidencyStatus" : "scotResident",
         "nextYearForecastResidencyStatus" : "scotResident"
        }
    """.stripMargin
  )

  "DESConnector"  should {
    "return correct residency status" when {
      "valid customer body is passed" in {

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        when(mockHttp.POST[HttpResponse, HttpResponse](Matchers.any(),Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, Some(residencyStatus))))

        val result = TestDesConnector.getResidencyStatus(CustomerDetails("LE241131B", "Jim", "Jimson", "1989-09-29"))

        result shouldBe ResidencyStatus("scotResident","scotResident")

      }

      "customer with nino: AE325433D is passed in" in {

        val result = DESConnector.getResidencyStatus(CustomerDetails("AE325433D", "Mary", "Brown", "1982-02-17"))
        result shouldBe AccountLockedResponse
      }
    }
  }


}
