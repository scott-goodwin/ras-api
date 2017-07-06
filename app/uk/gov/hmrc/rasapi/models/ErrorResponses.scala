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

package uk.gov.hmrc.rasapi.models

import play.api.libs.json.{JsValue, Json, Writes}

sealed abstract class ErrorResponse(
                                       val httpStatusCode: Int,
                                       val  errorCode: String,
                                       val message: String)

case object BadRequestResponse extends ErrorResponse(
  400,
  "BAD_REQUEST",
  "Bad Request") with JsonFormats

case object InvalidUUIDForbiddenResponse extends ErrorResponse(
  403,
  "INVALID_UUID",
  "The match has timed out and the UUID is no longer valid. " +
    "The match (POST to /match) will need to be repeated.") with JsonFormats

case object AccountLockedForbiddenResponse extends ErrorResponse(
  403,
  "INVALID_RESIDENCY_STATUS",
  "There is a problem with this member's account. Ask them to call HMRC."
) with JsonFormats

case object ErrorInternalServerError extends
  ErrorResponse(500, "INTERNAL_SERVER_ERROR", "Internal server error") with JsonFormats

case object ErrorNotFound extends ErrorResponse(404, "MATCHING_RESOURCE_NOT_FOUND", "A resource with the name in the request can not be found in the API") with JsonFormats


trait JsonFormats {
  implicit val errorResponseWrites = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message)
  }
}
