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

import java.util.UUID

import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatest.{WordSpec, _}
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.rasapi.models.Nino

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpResponse }

class CachingConnectorSpec extends WordSpec with MockitoSugar with ShouldMatchers with OneAppPerSuite {

  implicit val hc = HeaderCarrier()

  val mockHttpGet = mock[HttpGet]
  val uuid = UUID.randomUUID.toString
  val nino = Json.toJson(Nino("AB123456C"))

  object SUT extends CachingConnector {
    override val cachingBaseUrl = ""
    override val cachingUrl = ""
    override val httpGet: HttpGet = mockHttpGet
  }

  "getCachedData" should {

    "handle successful response when 200 is returned from caching service" in {

      when(mockHttpGet.GET[HttpResponse](Matchers.any())(Matchers.any(),Matchers.any())).
        thenReturn(Future.successful(HttpResponse(200, Some(nino))))

      val result = SUT.getCachedData(uuid)
      await(result).json shouldBe Json.toJson(Nino("AB123456C"))
    }

    "handle 404 error returned from caching service" in {

      when(mockHttpGet.GET[HttpResponse](Matchers.any())(Matchers.any(),Matchers.any())).
        thenReturn(Future.successful(HttpResponse(404, None)))

      val result = SUT.getCachedData(uuid)

      await(result).status shouldBe NOT_FOUND
    }

    "handle 500 error returned from caching service" in {

      when(mockHttpGet.GET[HttpResponse](Matchers.any())(Matchers.any(),Matchers.any())).
        thenReturn(Future.successful(HttpResponse(500, None)))


      val result = SUT.getCachedData(uuid)

      await(result).status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}


