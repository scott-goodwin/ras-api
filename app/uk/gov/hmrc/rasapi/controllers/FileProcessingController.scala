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

import play.api.Logger
import play.api.libs.json.JsSuccess
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.rasapi.models.CallbackData
import uk.gov.hmrc.rasapi.services.{FileProcessingService, SessionCacheService}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}


object FileProcessingController extends FileProcessingController {

  override val fileProcessingService: FileProcessingService = FileProcessingService
  override val sessionCacheService: SessionCacheService = SessionCacheService
}

trait FileProcessingController extends BaseController {

  val STATUS_AVAILABLE: String = "AVAILABLE"
  val STATUS_ERROR: String = "ERROR"

  val fileProcessingService: FileProcessingService
  val sessionCacheService: SessionCacheService

  def statusCallback(userId:String): Action[AnyContent] = Action.async {
    implicit request =>
      withValidJson.fold(Future.successful(BadRequest(""))){ callbackData =>
        Logger.warn(s"callback request received with status available: " )
        callbackData.status match {
          case STATUS_AVAILABLE =>
            if(Try(Future(fileProcessingService.processFile(callbackData))).isFailure) {
              sessionCacheService.updateFileSession(userId,callbackData,None)
            }
          case STATUS_ERROR => Logger.error(s"There is a problem with the file ERROR (${callbackData.fileId}), the status is:" +
            s" ${callbackData.status} and the reason is: ${callbackData.reason.get}")
            sessionCacheService.updateFileSession(userId,callbackData,None)
          case _ => Logger.error(s"There is a problem with the file (${callbackData.fileId}), the status is:" +
            s" ${callbackData.status}")
        }

        Future(Ok(""))
      }
  }

  private def withValidJson()(implicit request: Request[AnyContent]): Option[CallbackData] = {
    request.body.asJson match {
      case Some(json) =>
        Try(json.validate[CallbackData]) match {
          case Success(JsSuccess(payload, _)) => Some(payload)
          case _ => Logger.info(s"Json could not be parsed. Json Data: $json"); None
        }
      case _ => Logger.info("No json provided."); None
    }
  }
}