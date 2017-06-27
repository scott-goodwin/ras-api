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

import org.scalatest.mock.MockitoSugar
import org.scalatest.{WordSpec, _}
import uk.gov.hmrc.rasapi.models.CustomerDetails

class CachingConnectorSpec extends WordSpec with MockitoSugar with ShouldMatchers {

  val SUT = CachingConnector

  //TODO: The tests below will need to be updated after the migration of test data to stub

  "getCachedData" should {
    "return an option of some CustomerDetails" when {

      "a valid uuid (2800a7ab-fe20-42ca-98d7-c33f4133cfc2) is found" in {
        val result = SUT.getCachedData("2800a7ab-fe20-42ca-98d7-c33f4133cfc2")
        result shouldBe Some(CustomerDetails("LE241131B", "Jim", "Jimson", "1989-09-29"))
      }

      "a valid uuid (633e0ee7-315b-49e6-baed-d79c3dffe467) is found" in {
        val result = SUT.getCachedData("633e0ee7-315b-49e6-baed-d79c3dffe467")
        result shouldBe Some(CustomerDetails("BB123456B", "John", "Smith", "1975-05-25"))
      }

      "a valid uuid (77648d82-309e-484d-a310-d0ffd2997791) is found" in {
        val result = SUT.getCachedData("77648d82-309e-484d-a310-d0ffd2997791")
        result shouldBe Some(CustomerDetails("LR325154D", "Jane", "Doe", "1969-06-09"))
      }

      "a valid uuid (79f21755-8cd4-4785-9c10-13253f7a8bb6) is found" in {
        val result = SUT.getCachedData("79f21755-8cd4-4785-9c10-13253f7a8bb6")
        result shouldBe Some(CustomerDetails("CC123456C", "Joe", "Bloggs", "1982-02-17"))
      }

      "a valid uuid (2900a7ab-fe20-42ca-98d8-c33f4133cfc2) is found" in {
        val result = SUT.getCachedData("2900a7ab-fe20-42ca-98d8-c33f4133cfc2")
        result shouldBe Some(CustomerDetails("PC243122B", "Peter", "Armstrong", "1969-01-01"))
      }

      "a valid uuid (743e0ee7-315b-49e7-baed-d79c3dffe467) is found" in {
        val result = SUT.getCachedData("743e0ee7-315b-49e7-baed-d79c3dffe467")
        result shouldBe Some(CustomerDetails("EE123456D", "Steven", "Smith", "1947-08-15"))
      }

      "a valid uuid (88648d82-309e-484e-a310-d0ffd2997791) is found" in {
        val result = SUT.getCachedData("88648d82-309e-484e-a310-d0ffd2997791")
        result shouldBe Some(CustomerDetails("ZR132134C", "Simon", "Handyside", "1984-10-31"))
      }

      "a valid uuid (88648d82-309e-484d-a310-d0ffd2997792) is found" in {
        val result = SUT.getCachedData("88648d82-309e-484d-a310-d0ffd2997792")
        result shouldBe Some(CustomerDetails("SG123456D", "Linda", "Marshall", "1966-06-21"))
      }

      "a valid uuid (3000a7ab-fe20-42ca-98d9-c33f4133cfc2) is found" in {
        val result = SUT.getCachedData("3000a7ab-fe20-42ca-98d9-c33f4133cfc2")
        result shouldBe Some(CustomerDetails("CK355335C", "Kelly", "Thompson", "1990-02-15"))
      }

      "a valid uuid (853e0ee7-315b-49e8-baed-d79c3dffe467) is found" in {
        val result = SUT.getCachedData("853e0ee7-315b-49e8-baed-d79c3dffe467")
        result shouldBe Some(CustomerDetails("AR355335C", "Simon", "Handyside", "1984-10-31"))
      }

      "a valid uuid (99648d82-309e-484f-a310-d0ffd2997791) is found" in {
        val result = SUT.getCachedData("99648d82-309e-484f-a310-d0ffd2997791")
        result shouldBe Some(CustomerDetails("NW424252D", "Zack", "Jackson", "1966-04-04"))
      }

      "a valid uuid (99648d82-309e-484d-a310-d0ffd2997793) is found" in {
        val result = SUT.getCachedData("99648d82-309e-484d-a310-d0ffd2997793")
        result shouldBe Some(CustomerDetails("KA122234B", "Linda", "Marshall", "1966-06-21"))
      }

      "a valid uuid (3100a7ab-fe20-42ca-98d1-c33f4133cfc2) is found" in {
        val result = SUT.getCachedData("3100a7ab-fe20-42ca-98d1-c33f4133cfc2")
        result shouldBe Some(CustomerDetails("WK332122D", "Oscar", "Smith", "1986-06-14"))
      }

      "a valid uuid (963e0ee7-315b-49e-baed-d79c3dffe467) is found" in {
        val result = SUT.getCachedData("963e0ee7-315b-49e-baed-d79c3dffe467")
        result shouldBe Some(CustomerDetails("RW215443D", "Louise", "Oscar", "1966-04-04"))
      }

      "a valid uuid (11648d82-309e-484g-a310-d0ffd2997791) is found" in {
        val result = SUT.getCachedData("11648d82-309e-484g-a310-d0ffd2997791")
        result shouldBe Some(CustomerDetails("SE235112A", "Raj", "Patel", "1984-10-31"))
      }

      "a valid uuid (76648d82-309e-484d-a310-d0ffd2997794) is found" in {
        val result = SUT.getCachedData("76648d82-309e-484d-a310-d0ffd2997794")
        result shouldBe Some(CustomerDetails("AE325433D", "Mary", "Brown", "1982-02-17"))
      }

      "a valid uuid (76648d82-309e-484d-a310-d0ffd2997795) is found" in {
        val result = SUT.getCachedData("76648d82-309e-484d-a310-d0ffd2997795")
        result shouldBe Some(CustomerDetails("SG325433C", "Mary", "Poppins", "1952-02-17"))
      }
    }

    "return an option of none" when {
      "an invalid UUID is given" in {
        val uuid = "2800a7ab-fe20-42ca-98d7-c33f4133cgw4"

        val result = SUT.getCachedData(uuid)

        result shouldBe None
      }
    }
  }
}
