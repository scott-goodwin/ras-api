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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json.Json.toJson
import play.api.libs.streams.Streams
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.authorisedEnrolments
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.rasapi.config.RasAuthConnector
import uk.gov.hmrc.rasapi.metrics.Metrics
import uk.gov.hmrc.rasapi.repository.RasRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object FileController extends FileController{
  override val authConnector: AuthConnector = RasAuthConnector

}

trait FileController extends BaseController with AuthorisedFunctions{

  private val _contentType =   "application/csv"

  def serveFile(fileName:String):  Action[AnyContent] = Action.async {
    implicit request =>
      val apiMetrics = Metrics.register("file-serve-timer").time
      authorised(AuthProviders(GovernmentGateway) and (Enrolment(PSA_ENROLMENT) or Enrolment(PP_ENROLMENT))).retrieve(authorisedEnrolments) {
        enrols =>
          getFile(fileName).map { fileData =>
            if (fileData.isDefined) {
              Logger.debug("File repo enumerator received")
              val byteArray = Source.fromPublisher(Streams.enumeratorToPublisher(fileData.get.data.map(ByteString.fromArray)))
              apiMetrics.stop()
              Ok.sendEntity(HttpEntity.Streamed(byteArray, Some(fileData.get.length), Some(_contentType)))
                .withHeaders(CONTENT_DISPOSITION -> s"""attachment; filename="${fileName}"""",
                  CONTENT_LENGTH -> s"${fileData.get.length}",
                  CONTENT_TYPE -> _contentType)
            }
            else {
              Logger.error("Requested File not found to serve fileName is " + fileName)
              NotFound(toJson(ErrorNotFound))
            }

          }.recover {
            case ex: Throwable => Logger.error("Request failed with Exception " + ex.getMessage + " for file -> " + fileName)
              InternalServerError
          }
      } recoverWith{
        handleAuthFailure
          }
  }

  def remove(fileName:String):  Action[AnyContent] = Action.async {
    implicit request =>
      val apiMetrics = Metrics.register("file-remove-timer").time
      authorised(AuthProviders(GovernmentGateway) and (Enrolment(PSA_ENROLMENT) or Enrolment(PP_ENROLMENT))).retrieve(authorisedEnrolments) {
        enrols =>
          deleteFile(fileName).map{res=>  apiMetrics.stop()
            if(res) Ok("") else InternalServerError
          }.recover {
            case ex: Throwable => Logger.error("Request failed with Exception " + ex.getMessage + " for file -> " + fileName)
              InternalServerError
          }
      } recoverWith{
        handleAuthFailure
      }
  }

  private def handleAuthFailure(implicit request: Request[_]): PartialFunction[Throwable, Future[Result]] =
    PartialFunction[Throwable, Future[Result]]
  {
      case ex:InsufficientEnrolments => Logger.warn("Insufficient privileges")
        Metrics.registry.counter(UNAUTHORIZED.toString)

        Future.successful(Unauthorized(toJson(Unauthorised)))

      case ex:NoActiveSession => Logger.warn("Inactive session")
        Metrics.registry.counter(UNAUTHORIZED.toString)
        Future.successful(Unauthorized(toJson(InvalidCredentials)))
      case e => Logger.warn(s"Internal Error ${e.getCause}" )

        Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
    }

  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation

  def getFile(name:String) = RasRepository.filerepo.fetchFile(name)

  def deleteFile(name:String):Future[Boolean] = RasRepository.filerepo.removeFile(name)
  // $COVERAGE-ON$


}
