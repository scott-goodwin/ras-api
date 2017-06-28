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

import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.rasapi.config.WSHttp
import uk.gov.hmrc.rasapi.models.{CustomerDetails, Nino, ResidencyStatus}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait DesConnector {

  val http: HttpPost = WSHttp

  def getResidencyStatus(nino: Nino)(implicit hc: HeaderCarrier): Future[ResidencyStatus] = {

    val uri = "http://localhost:9670/ras-stubs/get-residency-status"

    val result =
      http.POST(uri, nino).map { response =>
          response.json.as[ResidencyStatus]
      }

    result

  }

}

object DesConnector extends DesConnector
