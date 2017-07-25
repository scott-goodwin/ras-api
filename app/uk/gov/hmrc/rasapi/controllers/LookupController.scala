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

package uk.gov.hmrc.rasapi.controllers

import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.rasapi.connectors.DesConnector
import uk.gov.hmrc.rasapi.models.InvalidUUIDForbiddenResponse
import uk.gov.hmrc.rasapi.connectors.CachingConnector
import uk.gov.hmrc.rasapi.models._
import play.api.libs.json.Json._
import play.api.Logger
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.rasapi.services.AuditService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait LookupController extends BaseController with HeaderValidator with RunMode {

  val cachingConnector: CachingConnector
  val desConnector: DesConnector
  val auditService: AuditService

  def getResidencyStatus(uuid: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>

      cachingConnector.getCachedData(uuid).flatMap ( customerCacheResponse =>
        customerCacheResponse.status match {
          case OK =>
            Logger.debug("Nino returned successfully [LookupController][getResidencyStatus]")
            val nino = customerCacheResponse.json.as[Nino]
            desConnector.getResidencyStatus(nino).map {
              case desResponse@(r: SuccessfulDesResponse) => {
                auditResponse(failureReason = None,
                              nino = Some(nino.nino),
                              residencyStatus = r.residencyStatus)
                Logger.debug("Residency status returned successfully [LookupController][getResidencyStatus]")
                Ok(toJson(r.residencyStatus))
              }
              case desResponse@AccountLockedResponse => {
                auditResponse(failureReason = Some(AccountLockedForbiddenResponse.errorCode),
                              nino = Some(nino.nino),
                              residencyStatus = None)
                Logger.debug("There was a problem with the account [LookupController][getResidencyStatus]")
                Forbidden(toJson(AccountLockedForbiddenResponse))
              }
              case desResponse => {
                auditResponse(failureReason = Some(ErrorInternalServerError.errorCode),
                              nino = Some(nino.nino),
                              residencyStatus = None)
                Logger.debug("Internal server error returned from DES [LookupController][getResidencyStatus]")
                InternalServerError(toJson(ErrorInternalServerError))
              }
            }
          case NOT_FOUND => {
            auditResponse(failureReason = Some(InvalidUUIDForbiddenResponse.errorCode),
                          nino = None,
                          residencyStatus = None)
            Logger.debug("Invalid uuid passed [LookupController][getResidencyStatus]")
            Future.successful(Forbidden(toJson(InvalidUUIDForbiddenResponse)))
          }
          case _ => {
            auditResponse(failureReason = Some(ErrorInternalServerError.errorCode),
                          nino = None,
                          residencyStatus = None)
            Logger.debug("Internal server error returned from cache [LookupController][getResidencyStatus]")
            Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
          }
        }
      )
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
                            residencyStatus: Option[ResidencyStatus])
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
      auditData = auditDataMap
    )
  }
}

object LookupController extends LookupController {
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val cachingConnector: CachingConnector = CachingConnector
  override val desConnector: DesConnector = DesConnector
  override val auditService: AuditService = AuditService
  // $COVERAGE-ON$
}
