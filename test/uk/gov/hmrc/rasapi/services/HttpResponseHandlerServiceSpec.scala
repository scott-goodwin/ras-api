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

import play.api.libs.json.Json
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.connectors.DesConnector
import uk.gov.hmrc.rasapi.models.{Nino, ResidencyStatus}
import org.scalatest.mock.MockitoSugar.mock
import org.mockito.Mockito.when
import org.mockito.Matchers.any

import scala.concurrent.Future

class HttpResponseHandlerServiceSpec extends UnitSpec{

  val mockDesConnector = mock[DesConnector]

  implicit val fakeHeaderCarrier: HeaderCarrier = HeaderCarrier()

  val SUT = new HttpResponseHandlerService {
    override val desConnector: DesConnector = mockDesConnector
  }

  "handleResidencyStatusResponse" should {
    "return a ResidencyStatusResponse object with a success object" when {
      "a 200 was returned with death indicator set to true" in {

        val responseJson = Json.parse(
          """
            {
            	"nino": "AB123456",
            	"deathDate": "1753-01-01",
            	"deathDateStatus": "not​ ​ verified",
            	"deseasedIndicator": true,
            	"currentYearResidencyStatus": "Uk",
            	"nextYearResidencyStatus": "Uk",
            	"processingDate": "2001-12-17T09:30:47Z"
            }
          """.stripMargin)

        val desResponse = HttpResponse(responseStatus = 200, responseJson = Some(responseJson))

        when(mockDesConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(desResponse))

        val expectedResult = Left(ResidencyStatus(currentYearResidencyStatus = "otherUKResident",
                                                  nextYearForecastResidencyStatus = "otherUKResident"))

        val nino = Nino("AB123456")

        val result = await(SUT.handleResidencyStatusResponse(nino))

        result shouldBe expectedResult
      }

      "a 200 was returned with death indicator set to false" in {

        val responseJson = Json.parse(
          """
            {
            	"nino": "AB123456",
            	"deseasedIndicator": false,
            	"currentYearResidencyStatus": "Uk",
            	"nextYearResidencyStatus": "Scottish",
            	"processingDate": "2001-12-17T09:30:47Z"
            }
          """.stripMargin)

        val desResponse = HttpResponse(responseStatus = 200, responseJson = Some(responseJson))

        when(mockDesConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(desResponse))

        val expectedResult = Left(ResidencyStatus(currentYearResidencyStatus = "otherUKResident",
                                                  nextYearForecastResidencyStatus = "scotResident"))

        val nino = Nino("AB123456")

        val result = await(SUT.handleResidencyStatusResponse(nino))

        result shouldBe expectedResult
      }
    }

    "return a residency object with a failure object" when {

      "a 400 response status is given" in {

        val responseJson = Json.parse(
          """
            {
            	"code": "INVALID_NINO",
              "reason": "Submission has not passed validation. Invalid parameter nino."
            }
          """.stripMargin)

        val desResponse = HttpResponse(responseStatus = 400, responseJson = Some(responseJson))

        when(mockDesConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(desResponse))

        val expectedResult = Right("")

        val nino = Nino("AB123456")

        val result = await(SUT.handleResidencyStatusResponse(nino))

        result shouldBe expectedResult
      }

      "a 404 (Nino not found) response status is given" in {

        val responseJson = Json.parse(
          """
            {
            	"code": "NINO_NOT_FOUND",
              "reason": "The NINO obtained from the digital front-end does not exist"
            }
          """.stripMargin)

        val desResponse = HttpResponse(responseStatus = 404, responseJson = Some(responseJson))

        when(mockDesConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(desResponse))

        val expectedResult = Right("")

        val nino = Nino("AB123456")

        val result = await(SUT.handleResidencyStatusResponse(nino))

        result shouldBe expectedResult
      }

      "a 503 (Unknown business error) response status is given" in {

        val responseJson = Json.parse(
          """
            {
            	"code": "UNKNOWN_BUSINESS_ERROR",
              "reason": "Unknown business error."
            }
          """.stripMargin)

        val desResponse = HttpResponse(responseStatus = 503, responseJson = Some(responseJson))

        when(mockDesConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(desResponse))

        val expectedResult = Right("")

        val nino = Nino("AB123456")

        val result = await(SUT.handleResidencyStatusResponse(nino))

        result shouldBe expectedResult
      }

      "a 404 (Not Found) response status is given" in {

        val responseJson = Json.parse(
          """
            {
            	"code": "NOT_FOUND",
              "reason": "Resource not found"
            }
          """.stripMargin)

        val desResponse = HttpResponse(responseStatus = 404, responseJson = Some(responseJson))

        when(mockDesConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(desResponse))

        val expectedResult = Right("")

        val nino = Nino("AB123456")

        val result = await(SUT.handleResidencyStatusResponse(nino))

        result shouldBe expectedResult
      }

      "a 500 (Server Error) response status is given" in {

        val responseJson = Json.parse(
          """
            {
            	"code": "SERVER_ERROR",
              "reason": "DES is currently experiencing problems that require live service intervention"
            }
          """.stripMargin)

        val desResponse = HttpResponse(responseStatus = 500, responseJson = Some(responseJson))

        when(mockDesConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(desResponse))

        val expectedResult = Right("")

        val nino = Nino("AB123456")

        val result = await(SUT.handleResidencyStatusResponse(nino))

        result shouldBe expectedResult
      }

      "a 503 (Service Unavailable) response status is given" in {

        val responseJson = Json.parse(
          """
            {
            	"code": "SERVICE_UNAVAILABLE",
              "reason": "Dependent systems are currently not responding"
            }
          """.stripMargin)

        val desResponse = HttpResponse(responseStatus = 503, responseJson = Some(responseJson))

        when(mockDesConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(desResponse))

        val expectedResult = Right("")

        val nino = Nino("AB123456")

        val result = await(SUT.handleResidencyStatusResponse(nino))

        result shouldBe expectedResult
      }
    }
  }
}
