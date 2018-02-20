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

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.models._
import uk.gov.hmrc.rasapi.services.AuditService

import scala.concurrent.Future

class DesConnectorSpec extends UnitSpec with OneAppPerSuite with BeforeAndAfter with MockitoSugar {

  implicit val hc = HeaderCarrier()

  before {
    reset(mockHttpGet)
  }

  val mockHttpGet = mock[HttpGet]
  val mockHttpPost = mock[HttpPost]
  val mockAuditService = mock[AuditService]
  implicit val format = ResidencyStatusFormats.successFormats

  object TestDesConnector extends DesConnector {
    override val httpPost: HttpPost = mockHttpPost
    override val desBaseUrl = ""
    override val edhUrl: String = "test-url"
    override val auditService: AuditService = mockAuditService
    override val allowNoNextYearStatus: Boolean = true
  }

  val individualDetails = IndividualDetails("LE241131B", "Joe", "Bloggs", new DateTime("1990-12-03"))
  val userId = "A123456"

  val residencyStatus = ResidencyStatus(currentYearResidencyStatus = "scotResident",
    nextYearForecastResidencyStatus = Some("scotResident"))

  val residencyStatusFailure = ResidencyStatusFailure("500", "HODS NOT AVAILABLE")

  val residencyStatusJson = Json.parse(
    """{
         "currentYearResidencyStatus" : "scotResident",
         "nextYearForecastResidencyStatus" : "scotResident"
        }
    """.stripMargin
  )

  "DESConnector sendDataToEDH" should {


    "DESConnector getResidencyStatus with matching" should {

      "handle successful response when 200 is returned from des and CY and CYPlusOne is present" in {

        val successresponse = ResidencyStatusSuccess(nino = "AB123456C", deathDate = Some(""), deathDateStatus = Some(""), deseasedIndicator = Some(false),
          currentYearResidencyStatus = "Uk", nextYearResidencyStatus = Some("Scottish"))
        when(mockHttpPost.POST[IndividualDetails, HttpResponse](any(), any(), any())(any(), any(), any(), any())).
          thenReturn(Future.successful(HttpResponse(200, Some(Json.toJson(successresponse)))))

        val result = await(TestDesConnector.getResidencyStatus(IndividualDetails("AB123456C", "JOHN", "SMITH", new DateTime("1990-02-21")), userId))
        result.isLeft shouldBe true
        result.left.get shouldBe ResidencyStatus("otherUKResident", Some("scotResident"))
      }

      "handle successful response when 200 is returned from des and only CY is present and feature toggle is turned on" in {

        val successresponse = ResidencyStatusSuccess(nino = "AB123456C", deathDate = Some(""), deathDateStatus = Some(""), deseasedIndicator = None,
          currentYearResidencyStatus = "Uk", nextYearResidencyStatus = None)
        when(mockHttpPost.POST[IndividualDetails, HttpResponse](any(), any(), any())(any(), any(), any(), any())).
          thenReturn(Future.successful(HttpResponse(200, Some(Json.toJson(successresponse)))))

        val result = await(TestDesConnector.getResidencyStatus(IndividualDetails("AB123456C", "JOHN", "SMITH", new DateTime("1990-02-21")), userId))
        result.isLeft shouldBe true
        result.left.get shouldBe ResidencyStatus("otherUKResident", None)
      }

      "handle successful response when 200 is returned from des and only CY is present and feature toggle is turned off" in {

        object TestDesConnector extends DesConnector {
          override val httpPost: HttpPost = mockHttpPost
          override val desBaseUrl = ""
          override val edhUrl: String = "test-url"
          override val auditService: AuditService = mockAuditService
          override val allowNoNextYearStatus: Boolean = false
        }

        val errorResponse = ResidencyStatusFailure("INTERNAL_SERVER_ERROR", "Internal server error")

        val successresponse = ResidencyStatusSuccess(nino = "AB123456C", deathDate = Some(""), deathDateStatus = Some(""), deseasedIndicator = Some(false),
          currentYearResidencyStatus = "Uk", nextYearResidencyStatus = None)
        when(mockHttpPost.POST[IndividualDetails, HttpResponse](any(), any(), any())(any(), any(), any(), any())).
          thenReturn(Future.successful(HttpResponse(200, Some(Json.toJson(successresponse)))))

        val result = await(TestDesConnector.getResidencyStatus(IndividualDetails("AB123456C", "JOHN", "SMITH", new DateTime("1990-02-21")), userId))
        result.isLeft shouldBe false
        result.right.get shouldBe errorResponse
      }

      "handle failure response (no match) from des" in {
        implicit val formatF = ResidencyStatusFormats.failureFormats
        val errorResponse = ResidencyStatusFailure("MATCHING_FAILED", "matching failed")
        when(mockHttpPost.POST[IndividualDetails, HttpResponse](any(), any(), any())(any(), any(), any(), any())).
          thenReturn(Future.successful(HttpResponse(404, Some(Json.toJson(errorResponse)))))

        val result = await(TestDesConnector.getResidencyStatus(IndividualDetails("AB123456C", "JOHN", "Lewis", new DateTime("1990-02-21")), userId))
        result.isLeft shouldBe false
        result.right.get shouldBe errorResponse
      }

      "handle success response but the person is deceased from des" in {
        val successResponse = ResidencyStatusSuccess(nino = "AB123456C", deathDate = Some("2017-12-25"), deathDateStatus = None,
          deseasedIndicator = Some(true), currentYearResidencyStatus = "Uk", nextYearResidencyStatus = Some("Uk"))
        when(mockHttpPost.POST[IndividualDetails, HttpResponse](any(), any(), any())(any(), any(), any(), any())).
          thenReturn(Future.successful(HttpResponse(200, Some(Json.toJson(successResponse)))))
        val expectedResult = ResidencyStatusFailure("DECEASED", "Individual is deceased")
        val result = await(TestDesConnector.getResidencyStatus(IndividualDetails("AB123456C", "JOHN", "Lewis", new DateTime("1990-02-21")), userId))
        result.isLeft shouldBe false
        result.right.get shouldBe expectedResult
      }

      "handle unexpected responses as 500 from des" in {

        when(mockHttpPost.POST[IndividualDetails, HttpResponse](any(), any(), any())(any(), any(), any(), any())).
          thenReturn(Future.successful(HttpResponse(500)))
        val errorResponse = ResidencyStatusFailure("INTERNAL_SERVER_ERROR", "Internal server error")

        val result = await(TestDesConnector.getResidencyStatus(IndividualDetails("AB123456C", "JOHN", "Lewis", new DateTime("1990-02-21")), userId))
        result.isLeft shouldBe false
        result.right.get shouldBe errorResponse
      }
    }

  }
}

