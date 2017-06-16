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

package uk.gov.hmrc.rasapi.services

import uk.gov.hmrc.rasapi.connectors.CachingConnector
import uk.gov.hmrc.rasapi.models.CustomerDetails


trait CachingService {

  val cachingConnector: CachingConnector

  def getData(uuid: String): Option[CustomerDetails] = cachingConnector.getCachedData(uuid)
}

object CachingService extends CachingService {

  override val cachingConnector: CachingConnector = CachingConnector
}
