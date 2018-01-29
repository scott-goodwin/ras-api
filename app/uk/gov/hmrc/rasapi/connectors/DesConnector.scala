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
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.rasapi.config.{AppContext, WSHttp}
import uk.gov.hmrc.rasapi.models._
import uk.gov.hmrc.rasapi.services.AuditService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import play.api.http.Status.INTERNAL_SERVER_ERROR


trait DesConnector extends ServicesConfig {

  val auditService: AuditService

  val httpGet: HttpGet
  val httpPost: HttpPost
  val desBaseUrl: String

  def getResidencyStatusUrl(nino: String): String

  val edhUrl: String

   val uk = "Uk"
   val scot = "Scottish"
   val otherUk = "otherUKResident"
   val scotRes = "scotResident"

  def sendDataToEDH(userId: String, nino: String, residencyStatus: ResidencyStatus)(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    httpPost.POST[EDHAudit, HttpResponse](url = edhUrl, body = EDHAudit(userId, nino,
      residencyStatus.currentYearResidencyStatus,residencyStatus.nextYearForecastResidencyStatus))(implicitly[Writes[EDHAudit]],
      implicitly[HttpReads[HttpResponse]], updateHeaderCarrier(hc), ec = MdcLoggingExecutionContext.fromLoggingDetails)
  }

  def getResidencyStatus(member: IndividualDetails, userId: String)(implicit hc: HeaderCarrier):
    Future[Either[ResidencyStatus, ResidencyStatusFailure]]  = {

    val uri = s"${desBaseUrl}/pension-scheme/customers/look-up"

    val result =  httpPost.POST[JsValue, HttpResponse](uri, Json.toJson[IndividualDetails](member), Seq())
    (implicitly[Writes[IndividualDetails]], implicitly[HttpReads[HttpResponse]], updateHeaderCarrier(hc),
      MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map (response => resolveResponse(response, userId, member.nino)).recover {
      case ex: NotFoundException =>
        Logger.error("[DesConnector] [getResidencyStatus] Matching Failed returned from connector")
        Right(ResidencyStatusFailure("MATCHING_FAILED", "The individual's details provided did not match with HMRC’s records."))
      case th: Throwable =>
        Logger.error("[DesConnector] [getResidencyStatus] Caught error occurred when calling the HoD", th)
        Right(ResidencyStatusFailure(INTERNAL_SERVER_ERROR.toString,"Internal server error"))
      case _ =>
        Logger.error("[DesConnector] [getResidencyStatus] Uncaught error occurred when calling the HoD")
        Right(ResidencyStatusFailure(INTERNAL_SERVER_ERROR.toString,"Internal server error"))
    }
  }

  private def resolveResponse(httpResponse: HttpResponse, userId: String, nino: NINO)(implicit hc: HeaderCarrier): Either[ResidencyStatus, ResidencyStatusFailure] =   {
    Try(httpResponse.json.as[ResidencyStatusSuccess](ResidencyStatusFormats.successFormats)) match {
      case Success(payload) =>

        if (payload.deseasedIndicator) {

          Right(ResidencyStatusFailure("DECEASED", "Individual is deceased"))
        } else {
          val currentStatus = payload.currentYearResidencyStatus.replace(uk, otherUk).replace(scot, scotRes)
          val nextYearStatus = payload.nextYearResidencyStatus.replace(uk, otherUk).replace(scot, scotRes)

          sendDataToEDH(userId, nino, ResidencyStatus(currentStatus, nextYearStatus)).map { httpResponse =>
            Logger.info("DesConnector - resolveResponse: Audited EDH response")
            auditEDHResponse(userId, nino, auditSuccess = true)
          } recover {
            case _ =>
              Logger.error("DesConnector - resolveResponse: Error returned from EDH")
              auditEDHResponse(userId, nino, auditSuccess = false)
          }

          Left(ResidencyStatus(currentStatus, nextYearStatus))
        }
      case Failure(_) =>
        Try(httpResponse.json.as[ResidencyStatusFailure](ResidencyStatusFormats.failureFormats)) match {
          case Success(data) => Logger.debug(s"DesFailureResponse from DES :${data}")
            Right(data)
          case Failure(ex) => Logger.error(s"Error from DES :${ex.getMessage}")
            Right(ResidencyStatusFailure(INTERNAL_SERVER_ERROR.toString,"HODS NOT AVAILABLE"))
        }
    }
  }

  private def auditEDHResponse(userId: String, nino: String, auditSuccess: Boolean)
                              (implicit hc: HeaderCarrier): Unit = {

    val auditDataMap = Map("userId" -> userId,
      "nino" -> nino,
      "edhAuditSuccess" -> auditSuccess.toString)

    auditService.audit(auditType = "ReliefAtSourceAudit",
      path = "PATH_NOT_DEFINED",
      auditData = auditDataMap
    )
  }

  private def updateHeaderCarrier(headerCarrier: HeaderCarrier) =
    headerCarrier.copy(extraHeaders = Seq("Environment" -> AppContext.desUrlHeaderEnv),
      authorization = Some(Authorization(s"Bearer ${AppContext.desAuthToken}")))
}

object DesConnector extends DesConnector {
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val auditService = AuditService
  override val httpGet: HttpGet = WSHttp
  override val httpPost: HttpPost = WSHttp
  override val desBaseUrl = baseUrl("des")
  override def getResidencyStatusUrl(nino: String) = String.format(AppContext.residencyStatusUrl, nino)
  override val edhUrl: String = desBaseUrl + AppContext.edhUrl
  // $COVERAGE-ON$
}
