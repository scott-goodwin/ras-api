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
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.rasapi.connectors.DesConnector
import uk.gov.hmrc.rasapi.models._
import play.api.libs.json.Json._
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.rasapi.services.AuditService
import uk.gov.hmrc.rasapi.config.{AppContext, RasAuthConnector}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.rasapi.helpers.ResidencyYearResolver
import uk.gov.hmrc.rasapi.metrics.Metrics
import uk.gov.hmrc.rasapi.utils.ErrorConverter

import scala.util.{Failure, Success, Try}

trait LookupController extends BaseController with HeaderValidator with RunMode with AuthorisedFunctions {

  val desConnector: DesConnector
  val auditService: AuditService
  val errorConverter: ErrorConverter
  val residencyYearResolver: ResidencyYearResolver

  def getCurrentDate: DateTime
  val allowDefaultRUK: Boolean

  val STATUS_DECEASED: String
  val STATUS_MATCHING_FAILED: String
  val STATUS_SERVICE_UNAVAILABLE: String

  def getResidencyStatus(): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      val apiMetrics = Metrics.responseTimer.time
      authorised(AuthProviders(GovernmentGateway) and (Enrolment(PSA_ENROLMENT) or Enrolment(PP_ENROLMENT))).retrieve(authorisedEnrolments) {
        enrols =>
          val id = getEnrolmentIdentifier(enrols)

          withValidJson(id,
            (individualDetails) => {

              desConnector.getResidencyStatus(individualDetails, id).map {
                case Left(residencyStatusResponse) =>
                  val residencyStatus = if (residencyYearResolver.isBetweenJanAndApril())
                                          updateResidencyResponse(residencyStatusResponse)
                                        else
                                          residencyStatusResponse.copy(nextYearForecastResidencyStatus = None)
                  auditResponse(failureReason = None,
                    nino = Some(individualDetails.nino),
                    residencyStatus = Some(residencyStatus),
                    userId = id)
                  Logger.debug(s"[LookupController][getResidencyStatus] Residency status returned successfully for userId ($id).")
                  apiMetrics.stop()
                  Ok(toJson(residencyStatus))

                case Right(matchingFailed) =>
                  matchingFailed.code match {
                    case STATUS_DECEASED =>
                      auditResponse(failureReason = Some(STATUS_DECEASED),
                        nino = Some(individualDetails.nino),
                        residencyStatus = None,
                        userId = id)
                      Logger.debug(s"[LookupController][getResidencyStatus] Individual is deceased for userId ($id).")
                      Metrics.registry.counter(FORBIDDEN.toString)
                      Forbidden(toJson(IndividualNotFound))
                    case STATUS_MATCHING_FAILED =>
                      auditResponse(failureReason = Some("MATCHING_FAILED"),
                        nino = Some(individualDetails.nino),
                        residencyStatus = None,
                        userId = id)
                      Logger.debug(s"[LookupController][getResidencyStatus] Individual not matched for userId ($id).")
                      Metrics.registry.counter(FORBIDDEN.toString)
                      Forbidden(toJson(IndividualNotFound))
                    case STATUS_SERVICE_UNAVAILABLE =>
                      auditResponse(failureReason = Some("SERVICE_UNAVAILABLE"),
                        nino = Some(individualDetails.nino),
                        residencyStatus = None,
                        userId = id)
                      Logger.debug(s"[LookupController][getResidencyStatus] Service unavailable for userId ($id).")
                      Metrics.registry.counter(SERVICE_UNAVAILABLE.toString)
                      ServiceUnavailable(toJson(ErrorServiceUnavailable))
                    case _ =>
                      auditResponse(failureReason = Some(ErrorInternalServerError.errorCode),
                        nino = Some(individualDetails.nino),
                        residencyStatus = None,
                        userId = id)
                      Logger.error(s"[LookupController][getResidencyStatus] Error returned from DES, error code: " +
                        s"${matchingFailed.code} for userId ($id).")
                      Metrics.registry.counter(INTERNAL_SERVER_ERROR.toString)
                      InternalServerError(toJson(ErrorInternalServerError))
                  }
              } recover {

                case th: Throwable =>
                  auditResponse(failureReason = Some(ErrorInternalServerError.errorCode),
                    nino = None,
                    residencyStatus = None,
                    userId = id)
                  Logger.error(s"[LookupController][getResidencyStatus] Error occurred for userId ($id), " +
                    s"Exception message: ${th.getMessage}", th)
                  Metrics.registry.counter(INTERNAL_SERVER_ERROR.toString)
                  InternalServerError(toJson(ErrorInternalServerError))
              }
            },
            errors => {
              Logger.info(s"The errors for userId ($id) are: " + errors.toString())
              Future.successful(BadRequest(toJson(ErrorBadRequestResponse(errorConverter.convert(errors)))))
            }
          )
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

  private def updateResidencyResponse(residencyStatus: ResidencyStatus): ResidencyStatus = {

    if (getCurrentDate.isBefore(new DateTime(2018, 4, 6, 0, 0, 0, 0)) && allowDefaultRUK)
      residencyStatus.copy(currentYearResidencyStatus = desConnector.otherUk)
    else
      residencyStatus
  }

  private def withValidJson (userId: String, onSuccess: (IndividualDetails) => Future[Result],
                             invalidCallback: (Seq[(JsPath, Seq[ValidationError])]) => Future[Result])
                            (implicit request: Request[AnyContent]): Future[Result] = {

    request.body.asJson match {
      case Some(json) =>
        Try(json.validate[IndividualDetails](IndividualDetails.individualDetailsReads)) match {
          case Success(JsSuccess(payload, _)) => {

            Try(onSuccess(payload)) match {
              case Success(result) => result
              case Failure(ex: Exception) =>
                Logger.error(s"[LookupController] An error occurred in Json payload validation for userId ($userId) ${ex.getMessage}")
                Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
            }
          }
          case Success(JsError(errors)) =>
            Logger.error(s"Json error in the request body")
            invalidCallback(errors)
          case Failure(e) => Logger.error(s"[LookupController] An error occurred due to ${e.getMessage} returning " +
            s"internal server error for userId ($userId).")
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
    * @param userId Identifies the user which made the request
    * @param request Object containing request made by the user
    * @param hc Headers
    */
  private def auditResponse(failureReason: Option[String], nino: Option[String],
                            residencyStatus: Option[ResidencyStatus], userId: String)
                           (implicit request: Request[AnyContent], hc: HeaderCarrier): Unit = {

    val ninoMap: Map[String, String] = nino.map(nino => Map("nino" -> nino)).getOrElse(Map())
    val nextYearStatusMap: Map[String, String] = if (residencyStatus.nonEmpty) residencyStatus.get.nextYearForecastResidencyStatus
                                                    .map(nextYear => Map("NextCYStatus" -> nextYear)).getOrElse(Map())
                                                 else Map()
    val auditDataMap: Map[String, String] = failureReason.map(reason => Map("successfulLookup" -> "false",
                                                                            "reason" -> reason)).
                                              getOrElse(Map(
                                                "successfulLookup" -> "true",
                                                "CYStatus" -> residencyStatus.get.currentYearResidencyStatus
                                              ) ++ nextYearStatusMap)

    auditService.audit(auditType = "ReliefAtSourceResidency",
      path = request.path,
      auditData = auditDataMap ++ Map("userIdentifier" -> userId, "requestSource" -> "API") ++ ninoMap
    )
  }
}

object LookupController extends LookupController {
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val desConnector: DesConnector = DesConnector
  override val auditService: AuditService = AuditService
  override val authConnector: AuthConnector = RasAuthConnector
  override val errorConverter: ErrorConverter = ErrorConverter
  override val residencyYearResolver: ResidencyYearResolver = ResidencyYearResolver
  override def getCurrentDate: DateTime = DateTime.now()
  override val allowDefaultRUK: Boolean = AppContext.allowDefaultRUK
  override val STATUS_DECEASED: String = AppContext.deceasedStatus
  override val STATUS_MATCHING_FAILED: String = AppContext.matchingFailedStatus
  override val STATUS_SERVICE_UNAVAILABLE: String = AppContext.serviceUnavailableStatus
  override val validateVersion: String => Boolean = _ == "1.0"
  // $COVERAGE-ON$
}
