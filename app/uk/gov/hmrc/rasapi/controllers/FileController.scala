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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json.Json.toJson
import play.api.libs.streams.Streams
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.rasapi.models.ErrorNotFound
import uk.gov.hmrc.rasapi.repository.RasRepository

import scala.concurrent.ExecutionContext.Implicits.global


object FileController extends FileController{

}

trait FileController extends BaseController{

  private val _contentType =   "application/octet-stream"

  def serveFile(fileName:String):  Action[AnyContent] = Action.async {
    implicit request =>

      getFile(fileName).map { fileData =>
        if (fileData.isDefined) {
          Logger.debug("File repo enumerator received")
          val byteArray = Source.fromPublisher(Streams.enumeratorToPublisher(fileData.get.data.map(ByteString.fromArray)))
          Ok.sendEntity(HttpEntity.Streamed(byteArray, Some(fileData.get.length), Some(_contentType)))
            .withHeaders(CONTENT_DISPOSITION -> s"""attachment; filename="${fileName}"""",
              CONTENT_LENGTH -> s"${fileData.get.length}",
              CONTENT_TYPE -> _contentType)
        }
        else
          {
            Logger.error("Requested File not found to serve fileName is " + fileName)
            NotFound(toJson(ErrorNotFound))}

        }.recover {
        case ex: Throwable =>   Logger.error("Request failed with Exception " + ex.getMessage + " for file -> " + fileName)
          InternalServerError
      }

  }
  def getFile(name:String) = RasRepository.filerepo.fetchFile(name)


}
