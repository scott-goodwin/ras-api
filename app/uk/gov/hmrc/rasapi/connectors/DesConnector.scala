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

import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.rasapi.config.{AppContext, WSHttp}
import uk.gov.hmrc.rasapi.models._
import uk.gov.hmrc.rasapi.services.AuditService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


trait DesConnector extends ServicesConfig {

  val auditService: AuditService

  val httpPost: HttpPost = WSHttp
  val desBaseUrl: String

  val edhUrl: String

  val allowNoNextYearStatus = AppContext.allowNoNextYearStatus

  val uk = "Uk"
  val scot = "Scottish"
  val otherUk = "otherUKResident"
  val scotRes = "scotResident"

  val error_InternalServerError = "INTERNAL_SERVER_ERROR"
  val error_Deceased = "DECEASED"
  val error_MatchingFailed = "MATCHING_FAILED"


  def getResidencyStatus(member: IndividualDetails, userId: String):
  Future[Either[ResidencyStatus, ResidencyStatusFailure]] = {

    implicit val rasHeaders = HeaderCarrier()

    val uri = s"${desBaseUrl}/individuals/residency-status/"

    val desHeaders = Seq("Environment" -> AppContext.desUrlHeaderEnv,
      "OriginatorId" -> "DA_RAS",
      "Content-Type" -> "application/json",
      "authorization" -> s"Bearer ${AppContext.desAuthToken}")

    val result = httpPost.POST[JsValue, HttpResponse](uri, Json.toJson[IndividualDetails](member), desHeaders)
    (implicitly[Writes[IndividualDetails]], implicitly[HttpReads[HttpResponse]], rasHeaders,
      MdcLoggingExecutionContext.fromLoggingDetails(rasHeaders))

    result.map(response => resolveResponse(response, userId, member.nino)).recover {
      case badRequestEx: BadRequestException =>
        Logger.error("[DesConnector] [getResidencyStatus] Bad Request returned from des. The details sent were not valid.")
        Right(ResidencyStatusFailure(error_InternalServerError, "Internal server error."))
      case notFoundEx: NotFoundException =>
        Logger.error("[DesConnector] [getResidencyStatus] Matching Failed returned from connector.")
        Right(ResidencyStatusFailure(error_MatchingFailed, "The pension scheme member's details do not match with HMRC's records."))
      case tooManyEx: TooManyRequestException =>
        Logger.error("[DesConnector] [getResidencyStatus] Request could not be sent 429 (Too Many Requests) was sent from the HoD.")
        Right(ResidencyStatusFailure(error_InternalServerError, "Internal server error."))
      case requestTimeOutEx: RequestTimeoutException =>
        Logger.error("[DesConnector] [getResidencyStatus] Request has timed out.")
        Right(ResidencyStatusFailure(error_InternalServerError, "Internal server error."))
      case th: Throwable =>
        Logger.error(s"[DesConnector] [getResidencyStatus] Caught error occurred when calling the HoD. Exception message: ${th.getMessage}")
        Right(ResidencyStatusFailure(error_InternalServerError, "Internal server error."))
      case _ =>
        Logger.error("[DesConnector] [getResidencyStatus] Uncaught error occurred when calling the HoD.")
        Right(ResidencyStatusFailure(error_InternalServerError, "Internal server error."))
    }
  }

  private def resolveResponse(httpResponse: HttpResponse, userId: String, nino: NINO)(implicit hc: HeaderCarrier): Either[ResidencyStatus, ResidencyStatusFailure] = {
    Try(httpResponse.json.as[ResidencyStatusSuccess](ResidencyStatusFormats.successFormats)) match {
      case Success(payload) =>

        payload.deseasedIndicator match {
          case Some(true) => Right(ResidencyStatusFailure(error_Deceased, "Individual is deceased"))
          case _ => {
            if (payload.nextYearResidencyStatus.isEmpty && !allowNoNextYearStatus) {
              val auditDataMap = Map("userId" -> userId,
                "nino" -> nino,
                "nextYearResidencyStatus" -> "NOT_PRESENT")

              auditService.audit(auditType = "ReliefAtSourceAudit_DES_Response",
                path = "PATH_NOT_DEFINED",
                auditData = auditDataMap
              )

              Right(ResidencyStatusFailure(error_InternalServerError, "Internal server error"))
            } else {
              val currentStatus = payload.currentYearResidencyStatus.replace(uk, otherUk).replace(scot, scotRes)
              val nextYearStatus: Option[String] = payload.nextYearResidencyStatus.map(_.replace(uk, otherUk).replace(scot, scotRes))

              Left(ResidencyStatus(currentStatus, nextYearStatus))
            }
          }
        }
      case Failure(_) =>
        Try(httpResponse.json.as[ResidencyStatusFailure](ResidencyStatusFormats.failureFormats)) match {
          case Success(data) => Logger.debug(s"DesFailureResponse from DES :${data}")
            Right(data)
          case Failure(ex) => Logger.error(s"Error from DES :${ex.getMessage}")
            Right(ResidencyStatusFailure(error_InternalServerError, "Internal server error."))
        }
    }
  }
}

object DesConnector extends DesConnector {
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val auditService = AuditService
  override val httpPost: HttpPost = WSHttp
  override val desBaseUrl = baseUrl("des")
  override val edhUrl: String = desBaseUrl + AppContext.edhUrl
  // $COVERAGE-ON$
}
