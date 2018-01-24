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

import play.api.libs.json.{JsValue, Json, Writes}

sealed abstract class ErrorResponse(
                                       val httpStatusCode: Int,
                                       val  errorCode: String,
                                       val message: String)

sealed abstract class ErrorResponseWithErrors( val httpStatusCode: Int,
                                               val errorCode: String,
                                               val message: String,
                                               val errors: Option[List[ErrorValidation]] = None)

case class ErrorValidation( errorCode: String,
                            message: String,
                            path: Option[String] = None)

case class ErrorBadRequestResponse(errs: List[ErrorValidation]) extends ErrorResponseWithErrors(
  400,
  "BAD_REQUEST",
  "Bad Request",
  errors = Some(errs))

case object BadRequestResponse extends ErrorResponse(
  400,
  "BAD_REQUEST",
  "Bad Request")

case object Unauthorised extends ErrorResponse(
  401,
  "UNAUTHORIZED",
  "Supplied OAuth token not authorised to access data for given tax identifier(s)")

case object InvalidCredentials extends ErrorResponse(
  401,
  "INVALID_CREDENTIALS",
  "Invalid OAuth token supplied for user-restricted or application-restricted resource (including expired token)")

case object IndividualNotFound extends ErrorResponse(
  403,
  "MATCHING_FAILED",
  "The individual's details provided did not match with HMRC’s records.")

// START OF TO BE DELETED
case object BadRequestInvalidFormatResponse extends ErrorResponse(
  400,
  "INVALID_FORMAT",
  "Invalid UUID format. Use the UUID provided.")

case object InvalidUUIDForbiddenResponse extends ErrorResponse(
  403,
  "INVALID_UUID",
  "The match has timed out and the UUID is no longer valid. " +
    "The match (POST to /match) will need to be repeated.")

case object AccountLockedForbiddenResponse extends ErrorResponse(
  403,
  "INVALID_RESIDENCY_STATUS",
  "There is a problem with this member's account. Ask them to call HMRC.")
// END OF TO BE DELETED

case object ErrorInternalServerError extends
  ErrorResponse(500, "INTERNAL_SERVER_ERROR", "Internal server error")

case object ErrorNotFound extends ErrorResponse(404, "NOT_FOUND", "Resource Not Found")
