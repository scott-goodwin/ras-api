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

import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.rasapi.connectors.CachingConnector
import uk.gov.hmrc.rasapi.models._
import play.api.libs.json.Json._
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.rasapi.services.{AuditService, HttpResponseHandlerService}
import uk.gov.hmrc.rasapi.config.RasAuthConnector
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.rasapi.metrics.Metrics

import scala.util.{Failure, Success, Try}

trait LookupController extends BaseController with HeaderValidator with RunMode with AuthorisedFunctions {

  val cachingConnector: CachingConnector
  val responseHandlerService: HttpResponseHandlerService
  val auditService: AuditService

  def getResidencyStatus(uuid: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      val apiMetrics = Metrics.responseTimer.time
      authorised(AuthProviders(GovernmentGateway) and (Enrolment(PSA_ENROLMENT) or Enrolment(PP_ENROLMENT))).retrieve(authorisedEnrolments) {
        enrols =>
          val id = getEnrolmentIdentifier(enrols)

          if (uuid.matches("^[0-9A-Fa-f]{8}(-[0-9A-Fa-f]{4}){3}-[0-9A-Fa-f]{12}$")) {

          cachingConnector.getCachedData(uuid).flatMap { customerCacheResponse =>
            customerCacheResponse.status match {
              case OK =>
                Logger.debug("[LookupController][getResidencyStatus] Nino returned successfully.")
                val nino = customerCacheResponse.json.as[Nino]
                responseHandlerService.handleResidencyStatusResponse(nino, id).map {
                  case Left(residencyStatus) => auditResponse(failureReason = None,
                    nino = Some(nino.nino),
                    residencyStatus = Some(residencyStatus),
                    userId = id)
                    Logger.debug("[LookupController][getResidencyStatus] Residency status returned successfully.")
                    apiMetrics.stop()
                    Ok(toJson(residencyStatus))
                  case Right(_) => auditResponse(failureReason = Some(ErrorInternalServerError.errorCode),
                    nino = Some(nino.nino),
                    residencyStatus = None,
                    userId = id)
                    Logger.error(s"[LookupController][getResidencyStatus] Internal server error due to error returned from DES.")
                    Metrics.registry.counter(INTERNAL_SERVER_ERROR.toString)
                    InternalServerError(toJson(ErrorInternalServerError))
                }
            }
          } recover {
            case _404: NotFoundException =>
              auditResponse(failureReason = Some(InvalidUUIDForbiddenResponse.errorCode),
                nino = None,
                residencyStatus = None,
                userId = id)
              Logger.debug("[LookupController][getResidencyStatus] UUID has timed out.")
              Metrics.registry.counter(FORBIDDEN.toString)

              Forbidden(toJson(InvalidUUIDForbiddenResponse))

            case th: Throwable =>
              auditResponse(failureReason = Some(ErrorInternalServerError.errorCode),
                nino = None,
                residencyStatus = None,
                userId = id)
              Logger.error(s"[LookupController][getResidencyStatus] Error while calling cache. " +
                s"Exception message: ${th.getMessage}", th)
              Metrics.registry.counter(INTERNAL_SERVER_ERROR.toString)
              InternalServerError(toJson(ErrorInternalServerError))
          }}
        else {
          Logger.debug("[LookupController][getResidencyStatus] invalid UUID specified")
            Metrics.registry.counter(BAD_REQUEST.toString)
            Future.successful(BadRequest(toJson(BadRequestInvalidFormatResponse)))
        }
      } recoverWith{
        case ex:InsufficientEnrolments => Logger.warn("Insufficient privileges")
          Metrics.registry.counter(UNAUTHORIZED.toString)

          Future.successful(Unauthorized(toJson(Unauthorised)))

        case ex:NoActiveSession => Logger.warn("Inactive session")
          Metrics.registry.counter(UNAUTHORIZED.toString)
          Future.successful(Unauthorized(toJson(InvalidCredentials)))
          case e => Logger.warn(s"Internal Error ${e.getCause}" )

          Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
      }

  }

  private def getEnrolmentIdentifier(enrols:Enrolments) = {
    enrols.enrolments.filter(res => (res.key == PSA_ENROLMENT || res.key == PP_ENROLMENT)).map(
      res => res.identifiers.head.value).head
  }

  private def withValidJson (onSuccess: (IndividualDetails) => Future[Result],
                             invalidCallback: (Seq[(JsPath, Seq[ValidationError])]) => Future[Result])
                            (implicit request: Request[AnyContent]): Future[Result] = {

    request.body.asJson match {
      case Some(json) =>
        Try(json.validate[IndividualDetails](IndividualDetails.individualDetailsReads)) match {
          case Success(JsSuccess(payload, _)) => {

            Try(onSuccess(payload)) match {
              case Success(result) => result
              case Failure(ex: Exception) =>
                Logger.error(s"CustomerMatchingController An error occurred in Json payload validation ${ex.getMessage}")
                Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
            }
          }
          case Success(JsError(errors)) =>
            invalidCallback(errors)
          case Failure(e) => Logger.error(s"CustomerMatchingController: An error occurred in customer-api due to ${e.getMessage} returning internal server error")
            Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
        }
      case None => Future.successful(BadRequest(toJson(BadRequestResponse)))
    }
  }

  /**
    * Audits the response, if failure reason is None then residencyStatus is Some (sucess) and vice versa (failure).
    * @param failureReason Optional message, present if the journey failed, else not
    * @param nino Optional user identifier, present if the customer-matching-cache call was a success, else not
    * @param residencyStatus Optional status object returned from the HoD, present if the journey succeeded, else not
    * @param request Object containing request made by the user
    * @param hc Headers
    */
  private def auditResponse(failureReason: Option[String], nino: Option[String],
                            residencyStatus: Option[ResidencyStatus], userId: String)
                           (implicit request: Request[AnyContent], hc: HeaderCarrier): Unit = {

    val ninoMap: Map[String, String] = nino.map(nino => Map("nino" -> nino)).getOrElse(Map())
    val auditDataMap: Map[String, String] = failureReason.map(reason => Map("successfulLookup" -> "false",
                                                                            "reason" -> reason) ++ ninoMap).
                                              getOrElse(Map(
                                                "successfulLookup" -> "true",
                                                "CYStatus" -> residencyStatus.get.currentYearResidencyStatus,
                                                "NextCYStatus" -> residencyStatus.get.nextYearForecastResidencyStatus
                                              ) ++ ninoMap)

    auditService.audit(auditType = "ReliefAtSourceResidency",
      path = request.path,
      auditData = auditDataMap ++ Map("userIdentifier" -> userId)
    )
  }
}

object LookupController extends LookupController {
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val cachingConnector: CachingConnector = CachingConnector
  override val responseHandlerService: HttpResponseHandlerService = HttpResponseHandlerService
  override val auditService: AuditService = AuditService
  override val authConnector: AuthConnector = RasAuthConnector
  // $COVERAGE-ON$
}
