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
import uk.gov.hmrc.rasapi.config.{AppContext, WSHttp}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpGet, HttpResponse}

trait CachingConnector extends ServicesConfig {

  val http: CoreGet
  val cachingBaseUrl: String
  val cachingUrl: String

  def getCachedData(uuid: String)(implicit hc: HeaderCarrier): Future[HttpResponse] ={

    val uri = cachingBaseUrl + cachingUrl + s"/$uuid"

    Logger.debug(s"[CachingConnector][getCachedData] making request to Customer Cache ($uri)")

    http.GET(uri)
  }
}

object CachingConnector extends CachingConnector {
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val http: CoreGet = WSHttp
  override val cachingBaseUrl = baseUrl("caching")
  override val cachingUrl = AppContext.cachingUrl
  // $COVERAGE-ON$
}