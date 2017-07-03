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

import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.Status.NOT_FOUND
import play.api.http.Status.FORBIDDEN
import play.api.http.Status.OK
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.rasapi.config.WSHttp
import uk.gov.hmrc.rasapi.models.{CustomerDetails, DesResponse, Nino, ResidencyStatus}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait DesConnector extends ServicesConfig {

  val http: HttpPost
  val desBaseUrl: String
  val cachingGetResidencyStatusUrl: String

  def getResidencyStatus(nino: Nino)(implicit hc: HeaderCarrier): Future[DesResponse] = {

    val uri = desBaseUrl + cachingGetResidencyStatusUrl

    http.POST(uri, nino).map { response =>
      response.status match {
        case 200 => DesResponse(OK, Some(response.json.as[ResidencyStatus]))
        case 403 => DesResponse(FORBIDDEN, None)
        case 404 => DesResponse(NOT_FOUND, None)
        case _ => DesResponse(INTERNAL_SERVER_ERROR, None)
      }
    }
  }
}

object DesConnector extends DesConnector{
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val http: HttpPost = WSHttp
  override val desBaseUrl = baseUrl("des")
  override val cachingGetResidencyStatusUrl = "/ras-stubs/get-residency-status"
  // $COVERAGE-ON$
}
