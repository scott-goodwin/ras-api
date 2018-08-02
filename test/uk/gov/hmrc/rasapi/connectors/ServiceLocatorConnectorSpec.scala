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

package uk.gov.hmrc.rasapi.connectors

import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito.when
import uk.gov.hmrc.api.domain.Registration
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, HttpResponse}


class ServiceLocatorConnectorSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  trait Setup {
    implicit val hc = HeaderCarrier()
    val serviceLocatorException = new RuntimeException

    val connector = new ServiceLocatorConnector {
      override val http = mock[HttpPost]
      override val appUrl: String = "http://api-microservice.service"
      override val appName: String = "api-microservice"
      override val serviceUrl: String = "https://SERVICE_LOCATOR"
      override val handlerOK: () => Unit = mock[Function0[Unit]]
      override val handlerError: Throwable => Unit = mock[Function1[Throwable, Unit]]
      override val metadata: Option[Map[String, String]] = Some(Map("third-party-api" -> "true"))
    }
  }

  "register" should {
    "register the JSON API Definition into the Service Locator" in new Setup {

      val registration = Registration(serviceName = "api-microservice", serviceUrl = "http://api-microservice.service", metadata = Some(Map("third-party-api" -> "true")))

      when(connector.http.POST[Registration, HttpResponse](
        Matchers.eq(s"${connector.serviceUrl}/registration"), Matchers.eq(registration), Matchers.eq(Seq("Content-Type"-> "application/json")))(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200)))

      connector.register.futureValue shouldBe true
      verify(connector.http).POST(Matchers.eq("https://SERVICE_LOCATOR/registration"), Matchers.eq(registration), Matchers.eq(Seq("Content-Type"-> "application/json")))(any(), any(), any(), any())
      verify(connector.handlerOK).apply()
      verify(connector.handlerError, never).apply(serviceLocatorException)
    }


    "fail registering in service locator" in new Setup {

      val registration = Registration(serviceName = "api-microservice", serviceUrl = "http://api-microservice.service", metadata = Some(Map("third-party-api" -> "true")))
      when(connector.http.POST(Matchers.eq(s"${connector.serviceUrl}/registration"), Matchers.eq(registration), Matchers.eq(Seq("Content-Type"-> "application/json")))(any(), any(), any(), any()))
        .thenReturn(Future.failed(serviceLocatorException))

      connector.register.futureValue shouldBe false
      verify(connector.http).POST(Matchers.eq("https://SERVICE_LOCATOR/registration"), Matchers.eq(registration), Matchers.eq(Seq("Content-Type"-> "application/json")))(any(), any(), any(), any())
      verify(connector.handlerOK, never).apply()
      verify(connector.handlerError).apply(serviceLocatorException)
    }

  }
}
