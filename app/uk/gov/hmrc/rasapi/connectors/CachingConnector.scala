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

import play.api.http.Status.{FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_ACCEPTABLE, NOT_FOUND, OK}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.rasapi.config.WSHttp
import uk.gov.hmrc.rasapi.models.{CustomerCacheResponse, CustomerDetails, Nino, ResidencyStatus}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait CachingConnector extends ServicesConfig {

  val http: HttpGet
  val cachingBaseUrl: String
  val cachingGetNinoUrl: String

  def getCachedData(uuid: String)(implicit hc: HeaderCarrier): Future[CustomerCacheResponse] ={

    val uri = cachingBaseUrl + cachingGetNinoUrl + s"/$uuid"

    http.GET(uri).map { response =>
      response.status match {
        case 200 => CustomerCacheResponse(OK, Some(response.json.as[Nino]))
        case 403 => CustomerCacheResponse(FORBIDDEN, None)
        case 404 => CustomerCacheResponse(NOT_FOUND, None)
        case _ => CustomerCacheResponse(INTERNAL_SERVER_ERROR, None)
      }
    }
  }

}

object CachingConnector extends CachingConnector{
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val http: HttpGet = WSHttp
  override val cachingBaseUrl = baseUrl("caching")
  override val cachingGetNinoUrl = "/customer-matching-cache/get-nino"
  // $COVERAGE-ON$
}