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
            desConnector.getResidencyStatus(customerCacheResponse.nino.getOrElse(Nino(""))).map {
              case desResponse@(r: SuccessfulDesResponse) => {
                auditResponse(uuid, customerCacheResponse, Some(desResponse), None)
                Logger.debug("Residency status returned successfully [LookupController][getResidencyStatus]")
                Ok(toJson(r.residencyStatus))
              }
              case desResponse@AccountLockedResponse => {
                auditResponse(uuid, customerCacheResponse, Some(desResponse), Some(AccountLockedForbiddenResponse.errorCode))
                Logger.debug("There was a problem with the account [LookupController][getResidencyStatus]")
                Forbidden(toJson(AccountLockedForbiddenResponse))
              }
              case desResponse => {
                auditResponse(uuid, customerCacheResponse, Some(desResponse), Some(ErrorInternalServerError.errorCode))
                Logger.debug("Internal server error returned from DES [LookupController][getResidencyStatus]")
                InternalServerError(toJson(ErrorInternalServerError))
              }
            }
          case FORBIDDEN => {
            auditResponse(uuid, customerCacheResponse, None, Some(InvalidUUIDForbiddenResponse.errorCode))
            Logger.debug("Invalid uuid passed [LookupController][getResidencyStatus]")
            Future.successful(Forbidden(toJson(InvalidUUIDForbiddenResponse)))
          }
          case _ => {
            auditResponse(uuid, customerCacheResponse, None, Some(ErrorInternalServerError.errorCode))
            Logger.debug("Internal server error returned from cache [LookupController][getResidencyStatus]")
            Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
          }
        }
      )
  }

  private def auditResponse(uuid: String, cachingResponse: CustomerCacheResponse, desResponse: Option[DesResponse],
                            failureReason: Option[String])
                           (implicit request: Request[AnyContent], hc: HeaderCarrier): Unit = {

    val dataMap = buildAuditDataMap(uuid, cachingResponse, desResponse)

    val auditType = desResponse.exists(residencyStatus => residencyStatus.residencyStatus.isDefined) match {
      case true => "residencyLookupSuccess"
      case false => "residencyLookupFailure"
    }

    val auditDataMap: Map[String, String] = failureReason.map(reason => dataMap ++ Map("failureReason" -> reason)).getOrElse(dataMap)

    auditService.audit(auditType = auditType,
      path = request.path,
      auditData = auditDataMap
    )
  }

  private def buildAuditDataMap(uuid: String, cachingResponse: CustomerCacheResponse, desResponse: Option[DesResponse]):
    Map[String, String] = {

    val nino = cachingResponse.nino.map (nino => nino.nino)
    val dataMap = Map("uuid" -> uuid,
                      "nino" -> nino.getOrElse("NO_NINO_DEFINED"))

    if (desResponse.isDefined) {

      val residencyStatus = desResponse.get.residencyStatus

      val dataMapAddition = dataMap ++ Map("residencyStatusDefined" -> residencyStatus.isDefined.toString)

      if (residencyStatus.isDefined)
        dataMapAddition ++ Map("CYResidencyStatus" -> residencyStatus.get.currentYearResidencyStatus,
                       "CYPlus1ResidencyStatus" -> residencyStatus.get.nextYearForecastResidencyStatus)
      else
        dataMapAddition
    } else
      dataMap ++ Map("residencyStatusDefined" -> "false")
  }
}

object LookupController extends LookupController {
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val cachingConnector: CachingConnector = CachingConnector
  override val desConnector: DesConnector = DesConnector
  override val auditService: AuditService = AuditService
  // $COVERAGE-ON$
}
