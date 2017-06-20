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

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.rasapi.connectors.{CachingConnector, DESConnector}
import uk.gov.hmrc.rasapi.models.{CustomerDetails, InvalidUUIDForbiddenResponse, ResidencyStatus}
import play.api.libs.json.Json._
import play.api.Logger
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait LookupController extends BaseController with HeaderValidator with RunMode {

  val cachingConnector: CachingConnector
  val desConnector: DESConnector

  def getResidencyStatus(uuid: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>

      cachingConnector.getCachedData(uuid) match {
        case Some(customerDetails) => {
          desConnector.getResidencyStatus(customerDetails) match {
            case Some(rs) => Future(Ok(toJson(rs)))
            case _ => {
              Logger.debug("Failed to retrieve residency status[LookupController][getResidencyStatus]")
              Future(InternalServerError)
            }
          }
        }
        case _ => {
          Logger.debug("Failed to retrieve customer details[LookupController][getResidencyStatus]")
          Future(Forbidden(toJson(InvalidUUIDForbiddenResponse)))
        }
      }
  }

}

object LookupController extends LookupController {
  override val cachingConnector: CachingConnector = CachingConnector
  override val desConnector: DESConnector = DESConnector
}
