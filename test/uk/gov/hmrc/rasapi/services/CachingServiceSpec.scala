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

import org.scalatest.{WordSpec, _}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.rasapi.connectors.CachingConnector
import uk.gov.hmrc.rasapi.models.CustomerDetails

import org.mockito.Matchers._
import org.mockito.Mockito._

class CachingServiceSpec extends WordSpec with MockitoSugar with ShouldMatchers {

  val mockCachingConnector = mock[CachingConnector]

  val SUT = new CachingService {

    override val cachingConnector = mockCachingConnector
  }

  "getData" should {
    "return a CustomerDetails object" when {
      "the uuid has been found" in {

        val uuid: String = "2800a7ab-fe20-42ca-98d7-c33f4133cfc2"

        val expectedCustomerDetails = Some(CustomerDetails(nino = "nino", firstName = "firstname",
                                                      lastName = "lastname", dateOfBirth = "dob"))

        when(mockCachingConnector.getCachedData(any())).thenReturn(expectedCustomerDetails)

        val result = SUT.getData(uuid)

        result shouldBe Some(CustomerDetails(nino = "nino", firstName = "firstname", lastName = "lastname", dateOfBirth = "dob"))
      }
    }

    "return an option of none" when {
      "the uuid could not be found" in {

        val uuid: String = "2800a7ab-fe20-42ca-98d7-c33f4133cfd1"

        val expectedCustomerDetails = None

        when(mockCachingConnector.getCachedData(any())).thenReturn(expectedCustomerDetails)

        val result = SUT.getData(uuid)

        result shouldBe None
      }
    }
  }
}
