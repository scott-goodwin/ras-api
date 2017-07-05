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

package uk.gov.hmrc.rasapi.connectors

import play.api.Logger
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.Status.NOT_FOUND
import play.api.http.Status.FORBIDDEN
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.rasapi.config.WSHttp
import uk.gov.hmrc.rasapi.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait DesConnector extends ServicesConfig {

  val http: HttpGet
  val desBaseUrl: String
  val cachingGetResidencyStatusUrl: String

  def getResidencyStatus(nino: Nino)(implicit hc: HeaderCarrier): Future[DesResponse] = {

    val customerNino = nino.nino
    val uri = desBaseUrl + cachingGetResidencyStatusUrl + s"/$customerNino"

    http.GET(uri).map { response =>
      response.status match {
        case 200 =>
          Logger.debug("Residency Status returned successfully [DesConnector][getResidencyStatus]")
          SuccessfulDesResponse(response.json.as[ResidencyStatus])
      }
    } recover {
      case exception: Upstream4xxResponse => {
        exception.upstreamResponseCode match {
          case 403 =>
            Logger.debug("There was a problem with the account [DesConnector][getResidencyStatus]")
            AccountLockedResponse
          case 404 =>
            Logger.debug("Resource not found [DesConnector][getResidencyStatus]")
            NotFoundResponse
        }
      }
      case exception: Upstream5xxResponse => {
        exception.upstreamResponseCode match {
          case _ =>
            Logger.debug("Internal server error [DesConnector][getResidencyStatus]")
            InternalServerErrorResponse
        }
      }
    }
  }
}

object DesConnector extends DesConnector{
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val http: HttpGet = WSHttp
  override val desBaseUrl = baseUrl("des")
  override val cachingGetResidencyStatusUrl = "/ras-stubs/get-residency-status"
  // $COVERAGE-ON$
}
