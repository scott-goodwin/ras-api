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

package uk.gov.hmrc.rasapi.controllers

import org.joda.time.DateTime
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.{JsPath, Json, Writes}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.rasapi.connectors.DesConnector
import org.mockito.Matchers.{eq => Meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import play.api.libs.functional.syntax.unlift
import play.api.libs.functional.syntax._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.rasapi.config.RasAuthConnector
import uk.gov.hmrc.rasapi.models._
import uk.gov.hmrc.rasapi.services.AuditService

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.helpers.ResidencyYearResolver
import uk.gov.hmrc.rasapi.utils.ErrorConverter

class LookupControllerSpec extends UnitSpec with MockitoSugar with OneAppPerSuite with BeforeAndAfter{

  implicit val hc = HeaderCarrier()

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")

  val mockDesConnector = mock[DesConnector]
  val mockAuditService = mock[AuditService]
  val mockAuthConnector = mock[RasAuthConnector]
  val mockResidencyYearResolver = mock[ResidencyYearResolver]

  when(mockDesConnector.otherUk).thenReturn("otherUKResident")
  when(mockDesConnector.scotRes).thenReturn("scotResident")

  val expectedNino: Nino = Nino("LE241131B")

  private val enrolmentIdentifier1 = EnrolmentIdentifier("PSAID", "A123456")
  private val enrolment1 = new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier1), state = "Activated", None)
  private val enrolmentIdentifier2 = EnrolmentIdentifier("PPID", "A123456")
  private val enrolment2 = new Enrolment(key = "HMRC-PP-ORG", identifiers = List(enrolmentIdentifier2), state = "Activated", None)
  private val enrolments = new Enrolments(Set(enrolment1,enrolment2))

  val successfulRetrieval: Future[Enrolments] = Future.successful(enrolments)

  val individualDetails = IndividualDetails("LE241131B", "Joe", "Bloggs", new DateTime("1990-12-03"))

  implicit val individualDetailssWrites: Writes[IndividualDetails] = (
    (JsPath \ "nino").write[String] and
      (JsPath \ "firstName").write[String] and
      (JsPath \ "lastName").write[String] and
      (JsPath \ "dateOfBirth").write[String].contramap[DateTime](date => date.toString("yyyy-MM-dd"))
    )(unlift(IndividualDetails.unapply))

  object TestLookupController extends LookupController {
    override val desConnector = mockDesConnector
    override val auditService: AuditService = mockAuditService
    override val authConnector: AuthConnector = mockAuthConnector
    override val errorConverter: ErrorConverter = ErrorConverter
    override val residencyYearResolver: ResidencyYearResolver = mockResidencyYearResolver
    override def getCurrentDate: DateTime = new DateTime(2018, 7, 6, 0, 0, 0, 0)
    override val allowDefaultRUK: Boolean = false
  }

  object TestLookupControllerFeb18 extends LookupController {
    override val desConnector = mockDesConnector
    override val auditService: AuditService = mockAuditService
    override val authConnector: AuthConnector = mockAuthConnector
    override val errorConverter: ErrorConverter = ErrorConverter
    override val residencyYearResolver: ResidencyYearResolver = mockResidencyYearResolver
    override def getCurrentDate: DateTime = new DateTime(2018, 2, 15, 0, 0, 0, 0)
    override val allowDefaultRUK: Boolean = true
  }

  object TestLookupControllerFeb19 extends LookupController {
    override val desConnector = mockDesConnector
    override val auditService: AuditService = mockAuditService
    override val authConnector: AuthConnector = mockAuthConnector
    override val errorConverter: ErrorConverter = ErrorConverter
    override val residencyYearResolver: ResidencyYearResolver = mockResidencyYearResolver
    override def getCurrentDate: DateTime = new DateTime(2019, 2, 15, 0, 0, 0, 0)
    override val allowDefaultRUK: Boolean = false
  }

  before{
    reset(mockAuditService)
  }

  "The lookup controller endpoint" should {

    "audit a successful lookup response" when {
      "a valid request has been submitted and the date is between january and april 2018" in {

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(true)

        val residencyStatus = ResidencyStatus("scotResident", Some("otherUKResident"))

        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Left(residencyStatus)))

        await(TestLookupControllerFeb18.getResidencyStatus()
          .apply(FakeRequest(Helpers.GET, s"/relief-at-source/customer/residency-status")
            .withHeaders(acceptHeader)
            .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites))))

        verify(mockAuditService).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq(s"/relief-at-source/customer/residency-status"),
          auditData = Meq(Map("successfulLookup" -> "true",
            "CYStatus" -> "otherUKResident",
            "NextCYStatus" -> "otherUKResident",
            "nino" -> "LE241131B",
            "userIdentifier" -> "A123456"))
        )(any())
      }

      "a valid request has been submitted and the date is between january and april 2019" in {

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(true)

        val residencyStatus = ResidencyStatus("scotResident", Some("otherUKResident"))

        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Left(residencyStatus)))

        await(TestLookupControllerFeb19.getResidencyStatus()
          .apply(FakeRequest(Helpers.GET, s"/relief-at-source/customer/residency-status")
            .withHeaders(acceptHeader)
            .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites))))

        verify(mockAuditService).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq(s"/relief-at-source/customer/residency-status"),
          auditData = Meq(Map("successfulLookup" -> "true",
            "CYStatus" -> "scotResident",
            "NextCYStatus" -> "otherUKResident",
            "nino" -> "LE241131B",
            "userIdentifier" -> "A123456"))
        )(any())
      }

      "a valid request has been submitted and the date is between april and december" in {

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(false)

        val residencyStatus = ResidencyStatus("otherUKResident", Some("otherUKResident"))

        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Left(residencyStatus)))

        await(TestLookupController.getResidencyStatus()
          .apply(FakeRequest(Helpers.GET, s"/relief-at-source/customer/residency-status")
            .withHeaders(acceptHeader)
            .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites))))

        verify(mockAuditService).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq(s"/relief-at-source/customer/residency-status"),
          auditData = Meq(Map("successfulLookup" -> "true",
            "CYStatus" -> "otherUKResident",
            "nino" -> "LE241131B",
            "userIdentifier" -> "A123456"))
        )(any())
      }

      "a valid request has been submitted and the date is between april and december and the individual is deceased" in {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(false)


        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Right(ResidencyStatusFailure("DECEASED", "Individual is deceased"))))

        await(TestLookupController.getResidencyStatus()
          .apply(FakeRequest(Helpers.GET, s"/relief-at-source/customer/residency-status")
            .withHeaders(acceptHeader)
            .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites))))

        verify(mockAuditService).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq(s"/relief-at-source/customer/residency-status"),
          auditData = Meq(Map("successfulLookup" -> "false",
            "reason" -> "DECEASED",
            "nino" -> "LE241131B",
            "userIdentifier" -> "A123456"))
        )(any())
      }
    }

    "audit a unsuccessful lookup response" when {

      "a no match is returned" in {

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(),any())).thenReturn(successfulRetrieval)

        val residencyStatusFailure = ResidencyStatusFailure("MATCHING_FAILED", "")

        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Right(residencyStatusFailure)))

        await(TestLookupController.getResidencyStatus()
          .apply(FakeRequest(Helpers.GET, s"/relief-at-source/customer/residency-status")
            .withHeaders(acceptHeader)
            .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites))))

        verify(mockAuditService).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq(s"/relief-at-source/customer/residency-status"),
          auditData = Meq(Map("nino" -> "LE241131B",
            "successfulLookup" -> "false",
            "reason" -> "MATCHING_FAILED",
            "userIdentifier" -> "A123456"))
        )(any())
      }

      "a problem has occurred in the Head of Duty (HoD) system" in {
  
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(),any())).thenReturn(successfulRetrieval)

        val residencyStatusFailure = ResidencyStatusFailure("INTERNAL_SERVER_ERROR", "")

        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Right(residencyStatusFailure)))
  
        await(TestLookupController.getResidencyStatus()
          .apply(FakeRequest(Helpers.GET, s"/relief-at-source/customer/residency-status")
          .withHeaders(acceptHeader)
          .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites))))
  
        verify(mockAuditService).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq(s"/relief-at-source/customer/residency-status"),
          auditData = Meq(Map("nino" -> "LE241131B",
                              "successfulLookup" -> "false",
                              "reason" -> "INTERNAL_SERVER_ERROR",
                              "userIdentifier" -> "A123456"))
        )(any())
      }
    }
  }

  "LookupController" should {

    "return status 200 with correct residency status json" when {

      "a valid request payload is given and the date of the request is between january and april 2018" in {

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(),any())).thenReturn(successfulRetrieval)

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(true)

        val residencyStatus = ResidencyStatus("scotResident", Some("otherUKResident"))

        val expectedJsonResult = Json.parse(
          """
            {
              "currentYearResidencyStatus" : "otherUKResident",
              "nextYearForecastResidencyStatus" : "otherUKResident"
            }
          """.stripMargin)

        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Left(residencyStatus)))

        val result = TestLookupControllerFeb18.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader)
          .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites)))

        status(result) shouldBe OK
        contentAsJson(result) shouldBe expectedJsonResult
      }

      "a valid request payload is given and the date of the request is between january and april 2019" in {

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(),any())).thenReturn(successfulRetrieval)

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(true)

        val residencyStatus = ResidencyStatus("scotResident", Some("otherUKResident"))

        val expectedJsonResult = Json.parse(
          """
            {
              "currentYearResidencyStatus" : "scotResident",
              "nextYearForecastResidencyStatus" : "otherUKResident"
            }
          """.stripMargin)

        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Left(residencyStatus)))

        val result = TestLookupControllerFeb19.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader)
          .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites)))

        status(result) shouldBe OK
        contentAsJson(result) shouldBe expectedJsonResult
      }

      "a valid request payload is given with a nino which is 9 characters in length e.g. AA123456A" in {

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(),any())).thenReturn(successfulRetrieval)

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(true)

        val residencyStatus = ResidencyStatus("otherUKResident", Some("otherUKResident"))

        val expectedJsonResult = Json.parse(
          """
            {
              "currentYearResidencyStatus" : "otherUKResident",
              "nextYearForecastResidencyStatus" : "otherUKResident"
            }
          """.stripMargin)

        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Left(residencyStatus)))

        val result = TestLookupController.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader)
          .withJsonBody(Json.toJson(individualDetails.copy(nino = individualDetails.nino.toLowerCase))))

        status(result) shouldBe OK
        contentAsJson(result) shouldBe expectedJsonResult
      }

      "a valid request payload is given and the date of the request is between april and december" in {

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(),any())).thenReturn(successfulRetrieval)

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(false)

        val residencyStatus = ResidencyStatus("otherUKResident", Some("otherUKResident"))

        val expectedJsonResult = Json.parse(
          """
            {
              "currentYearResidencyStatus" : "otherUKResident"
            }
          """.stripMargin)

        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Left(residencyStatus)))

        val result = TestLookupController.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader)
          .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites)))

        status(result) shouldBe OK
        contentAsJson(result) shouldBe expectedJsonResult
      }

      "a valid request payload is given with a nino which is 8 characters in length e.g. AA123456" in {

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(),any())).thenReturn(successfulRetrieval)
        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(true)

        val residencyStatus = ResidencyStatus("otherUKResident", Some("otherUKResident"))

        val expectedJsonResult = Json.parse(
          """
            {
              "currentYearResidencyStatus" : "otherUKResident",
              "nextYearForecastResidencyStatus" : "otherUKResident"
            }
          """.stripMargin)

        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Left(residencyStatus)))

        val result = TestLookupController.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader)
          .withJsonBody(Json.toJson(individualDetails.copy(nino = "AA123456"))))

        status(result) shouldBe OK
        contentAsJson(result) shouldBe expectedJsonResult
      }
    }

    "return status 400" when {
      "an invalid payload is provided" in {

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(),any())).thenReturn(successfulRetrieval)

        val invalidPayload = Json.parse(
          """
            |{
            |   "firstName": "Joe",
            |   "nino": "AA123243E",
            |   "dob": ""
            |}
          """.stripMargin
        )

        val expectedJsonResult = Json.parse(
          """
            |{
            |  "code": "BAD_REQUEST",
            |  "message": "Bad Request",
            |  "errors": [
            |     {
            |       "code": "INVALID_FORMAT",
            |       "message": "Invalid format has been used",
            |       "path": "/nino"
            |     },
            |     {
            |       "code": "MISSING_FIELD",
            |       "message": "This field is required",
            |       "path": "/lastName"
            |     },
            |     {
            |       "code": "MISSING_FIELD",
            |       "message": "This field is required",
            |       "path": "/dateOfBirth"
            |     }
            |  ]
            |}
          """.stripMargin)

        val result = TestLookupController.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/")
          .withHeaders(acceptHeader)
          .withJsonBody(invalidPayload))

        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe expectedJsonResult
      }
    }

    "return status 401 (Unauthorised)" when {
      "a valid lookup request has been submitted with no PSA or PP enrolments" in {

        when(mockAuthConnector.authorise[Option[String]](any(), any())(any(),any())).thenReturn(Future.failed(new InsufficientEnrolments))

        val authorisationHeader: (String, String) = (HeaderNames.AUTHORIZATION, "Bearer ABC")

        val expectedJsonResult = Json.parse(
          """
            |{
            |  "code": "UNAUTHORIZED",
            |  "message": "Supplied OAuth token not authorised to access data for given tax identifier(s)"
            |}
          """.stripMargin)

        val result = TestLookupController.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/").
          withHeaders(acceptHeader, authorisationHeader))

        status(result) shouldBe UNAUTHORIZED
        contentAsJson(result) shouldBe expectedJsonResult
      }

      "a valid lookup request has been submitted with no authorization header present" in {

        when(mockAuthConnector.authorise[Option[String]](any(), any())(any(),any())).thenReturn(Future.failed(new MissingBearerToken))

        val expectedJsonResult = Json.parse(
          """
            |{
            |  "code": "INVALID_CREDENTIALS",
            |  "message": "Invalid OAuth token supplied for user-restricted or application-restricted resource (including expired token)"
            |}
          """.stripMargin)

        val result = TestLookupController.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/")
          .withHeaders(acceptHeader)
          .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites)))

        status(result) shouldBe UNAUTHORIZED
        contentAsJson(result) shouldBe expectedJsonResult
      }

      "a valid lookup request has been submitted with no value declared in the authorization header" in {

        when(mockAuthConnector.authorise[Option[String]](any(), any())(any(),any())).thenReturn(Future.failed(new InvalidBearerToken))

        val authorisationHeader: (String, String) = (HeaderNames.AUTHORIZATION, "")

        val expectedJsonResult = Json.parse(
          """
            |{
            |  "code": "INVALID_CREDENTIALS",
            |  "message": "Invalid OAuth token supplied for user-restricted or application-restricted resource (including expired token)"
            |}
          """.stripMargin)

        val result = TestLookupController.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/")
          .withHeaders(acceptHeader, authorisationHeader)
          .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites)))

        status(result) shouldBe UNAUTHORIZED
        contentAsJson(result) shouldBe expectedJsonResult
      }

      "a valid lookup request has been submitted with an expired bearer token in the authorization header" in {
        // The bearer token used in this test is not valid but for purposes of testing is being treated as a valid bearer token.

        when(mockAuthConnector.authorise[Option[String]](any(), any())(any(),any())).thenReturn(Future.failed(new BearerTokenExpired))

        val authorisationHeader: (String, String) = (HeaderNames.AUTHORIZATION, "")

        val expectedJsonResult = Json.parse(
          """
            |{
            |  "code": "INVALID_CREDENTIALS",
            |  "message": "Invalid OAuth token supplied for user-restricted or application-restricted resource (including expired token)"
            |}
          """.stripMargin)

        val result = TestLookupController.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/")
          .withHeaders(acceptHeader, authorisationHeader)
          .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites)))

        status(result) shouldBe UNAUTHORIZED
        contentAsJson(result) shouldBe expectedJsonResult
      }

      "a valid match request has been submitted with an invalid bearer token in the authorization header" in {

        when(mockAuthConnector.authorise[Option[String]](any(), any())(any(),any())).thenReturn(Future.failed(new SessionRecordNotFound))

        val authorisationHeader: (String, String) = (HeaderNames.AUTHORIZATION, "Bearer ABC")

        val expectedJsonResult = Json.parse(
          """
            |{
            |  "code": "INVALID_CREDENTIALS",
            |  "message": "Invalid OAuth token supplied for user-restricted or application-restricted resource (including expired token)"
            |}
          """.stripMargin)

        val result = TestLookupController.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/")
          .withHeaders(acceptHeader, authorisationHeader)
          .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites)))

        status(result) shouldBe UNAUTHORIZED
        contentAsJson(result) shouldBe expectedJsonResult
      }
    }

    "return status 403" when {
      "MATCHING_FAILED is returned from the connector" in {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(),any())).thenReturn(successfulRetrieval)

        val expectedJsonResult = Json.parse(
          """
            |{
            |  "code": "MATCHING_FAILED",
            |  "message": "The individual's details provided did not match with HMRC’s records."
            |}
          """.stripMargin)

        val residencyStatusFailure = ResidencyStatusFailure("MATCHING_FAILED", "")

        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Right(residencyStatusFailure)))

        val result = TestLookupController.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/")
          .withHeaders(acceptHeader)
          .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites)))

        status(result) shouldBe FORBIDDEN
        contentAsJson(result) shouldBe expectedJsonResult
      }
    }

    "return status 500" when {

      "when residency status is not returned from the response handler service" in {

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(),any())).thenReturn(successfulRetrieval)

        val expectedJsonResult = Json.parse(
          """
            |{
            |  "code": "INTERNAL_SERVER_ERROR",
            |  "message": "Internal server error"
            |}
          """.stripMargin)

        val residencyStatusFailure = ResidencyStatusFailure("INTERNAL_SERVER_ERROR", "")

        when(mockDesConnector.getResidencyStatus(any(), any())).thenReturn(Future.successful(Right(residencyStatusFailure)))

        val result = TestLookupController.getResidencyStatus().apply(FakeRequest(Helpers.GET, "/")
          .withHeaders(acceptHeader)
          .withJsonBody(Json.toJson(individualDetails)(individualDetailssWrites)))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe expectedJsonResult
      }
    }
  }

}

