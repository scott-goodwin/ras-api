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
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.rasapi.config.{AppContext, WSHttp}

import scala.concurrent.Future

trait CachingConnector extends ServicesConfig {

  val httpGet: HttpGet
  val cachingBaseUrl: String
  val cachingUrl: String

  def getCachedData(uuid: String)(implicit hc: HeaderCarrier): Future[HttpResponse] ={

    val uri = cachingBaseUrl + cachingUrl + s"/$uuid"

    Logger.debug("[CachingConnector][getCachedData] making request to Customer Cache")

    httpGet.GET(uri)
  }
}

object CachingConnector extends CachingConnector{
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val httpGet: HttpGet = WSHttp
  override val cachingBaseUrl = baseUrl("caching")
  override val cachingUrl = AppContext.cachingUrl
  // $COVERAGE-ON$
}