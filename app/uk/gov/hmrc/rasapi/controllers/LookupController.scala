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

import java.util.concurrent.TimeUnit

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.rasapi.connectors.{CachingConnector, DesConnector}
import uk.gov.hmrc.rasapi.models.{CustomerDetails, InvalidUUIDForbiddenResponse, ResidencyStatus}
import uk.gov.hmrc.rasapi.connectors.CachingConnector
import uk.gov.hmrc.rasapi.models._
import play.api.libs.json.Json._
import play.api.Logger

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

trait LookupController extends BaseController with HeaderValidator with RunMode {

  val cachingConnector: CachingConnector
  val desConnector: DesConnector

  def getResidencyStatus(uuid: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>

      cachingConnector.getCachedData(uuid).flatMap ( customerCacheResponse =>
        customerCacheResponse.status match {
          case OK => desConnector.getResidencyStatus(customerCacheResponse.nino.getOrElse(Nino(""))).map(desResponse =>
            desResponse match {
              case r: SuccessfulDesResponse => Ok(toJson(r.residencyStatus))
              case AccountLockedResponse => Forbidden(toJson(AccountLockedForbiddenResponse))
              case NotFoundResponse => NotFound(toJson(ErrorNotFound))
              case _ => InternalServerError(toJson(ErrorInternalServerError))
            }
          )
          case FORBIDDEN => Future.successful(Forbidden(toJson(InvalidUUIDForbiddenResponse)))
          case NOT_FOUND => Future.successful(NotFound(toJson(ErrorNotFound)))
          case _ => Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
        }
      )
  }
}

object LookupController extends LookupController {
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val cachingConnector: CachingConnector = CachingConnector
  override val desConnector: DesConnector = DesConnector
  // $COVERAGE-ON$
}
