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

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito.when
import uk.gov.hmrc.api.domain.Registration
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.{CorePost, HeaderCarrier, HttpPost, HttpResponse}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.rasapi.models.EDHAudit


class ServiceLocatorConnectorSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  implicit val hc = HeaderCarrier()
  val serviceLocatorException = new RuntimeException

  val mockHttp = mock[CorePost]

  object TestServiceLocatorConnector extends ServiceLocatorConnector {
    override val http = mockHttp
    override val appUrl: String = "http://api-microservice.service"
    override val appName: String = "api-microservice"
    override val serviceUrl: String = "https://SERVICE_LOCATOR"
    override val handlerOK: () => Unit = mock[Function0[Unit]]
    override val handlerError: Throwable => Unit = mock[Function1[Throwable, Unit]]
    override val metadata: Option[Map[String, String]] = Some(Map("third-party-api" -> "true"))
  }

  "register" should {
    "register the JSON API Definition into the Service Locator" in {

      val registration = Registration(serviceName = "api-microservice", serviceUrl = "http://api-microservice.service", metadata = Some(Map("third-party-api" -> "true")))

      when(mockHttp.POST[Registration,HttpResponse]("https://SERVICE_LOCATOR/registration", registration, Seq("Content-Type"-> "application/json"))).thenReturn(Future.successful(HttpResponse(200)))

      TestServiceLocatorConnector.register.futureValue shouldBe true
      verify(TestServiceLocatorConnector.http).POST("https://SERVICE_LOCATOR/registration", registration, Seq("Content-Type"-> "application/json"))
      verify(TestServiceLocatorConnector.handlerOK).apply()
      verify(TestServiceLocatorConnector.handlerError, never).apply(serviceLocatorException)
    }


    "fail registering in service locator" in {

      val registration = Registration(serviceName = "api-microservice", serviceUrl = "http://api-microservice.service", metadata = Some(Map("third-party-api" -> "true")))

      when(mockHttp.POST("https://SERVICE_LOCATOR/registration", registration, Seq("Content-Type"-> "application/json"))).thenReturn(Future.failed(serviceLocatorException))

      TestServiceLocatorConnector.register.futureValue shouldBe false
      verify(TestServiceLocatorConnector.http).POST("https://SERVICE_LOCATOR/registration", registration, Seq("Content-Type"-> "application/json"))
      verify(TestServiceLocatorConnector.handlerOK, never).apply()
      verify(TestServiceLocatorConnector.handlerError).apply(serviceLocatorException)
    }

  }
}
