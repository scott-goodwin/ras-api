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

import org.scalatest.{ShouldMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.rasapi.models._

class DESConnectorSpec extends WordSpec with OneAppPerSuite with ShouldMatchers{

  //TODO: The tests below will need to be updated after the migration of test data to stubs

  "DESConnector" should {
    "return correct residency status" when {

      "customer with nino: LE241131B is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("LE241131B", "Jim", "Jimson", "1989-09-29"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("otherUKResident","otherUKResident"))
      }

      "customer with nino: BB123456B is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("BB123456B", "John", "Smith", "1975-05-25"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("otherUKResident","scotResident"))
      }

      "customer with nino: LR325154D is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("LR325154D", "Jane", "Doe", "1969-06-09"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("scotResident", "otherUKResident"))
      }

      "customer with nino: CC123456C is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("CC123456C", "Joe", "Bloggs", "1982-02-17"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("scotResident", "scotResident"))
      }

      "customer with nino: PC243122B is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("PC243122B", "Peter", "Armstrong", "1969-01-01"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("otherUKResident","otherUKResident"))
      }

      "customer with nino: EE123456D is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("EE123456D", "Steven", "Smith", "1947-08-15"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("otherUKResident","scotResident"))
      }

      "customer with nino: ZR132134C is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("ZR132134C", "Simon", "Handyside", "1984-10-31"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("scotResident", "otherUKResident"))
      }

      "customer with nino: SG123456D is passed in" in {

        val result = DESConnector.getResidencyStatus(CustomerDetails("SG123456D", "Linda", "Marshall", "1966-06-21"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("scotResident", "scotResident"))
      }

      "customer with nino: CK355335C is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("CK355335C", "Kelly", "Thompson", "1990-02-15"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("otherUKResident","otherUKResident"))
      }

      "customer with nino: AR355335C is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("AR355335C", "Simon", "Handyside", "1984-10-31"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("otherUKResident","scotResident"))
      }

      "customer with nino: NW424252D is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("NW424252D", "Zack", "Jackson", "1966-04-04"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("scotResident", "otherUKResident"))
      }

      "customer with nino: KA122234B is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("KA122234B", "Linda", "Marshall", "1966-06-21"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("scotResident", "scotResident"))
      }

      "customer with nino: WK332122D is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("WK332122D", "Oscar", "Smith", "1986-06-14"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("otherUKResident","otherUKResident"))
      }

      "customer with nino: RW215443D is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("RW215443D", "Louise", "Oscar", "1966-04-04"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("otherUKResident","scotResident"))
      }

      "customer with nino: SE235112A is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("SE235112A", "Raj", "Patel", "1984-10-31"))
        result shouldBe SuccessfulNPSResponse(ResidencyStatus("scotResident", "otherUKResident"))
      }

      "customer with nino: AE325433D is passed in" in {

        val result = DESConnector.getResidencyStatus(CustomerDetails("AE325433D", "Mary", "Brown", "1982-02-17"))
        result shouldBe AccountLockedResponse
      }

      "customer with nino: DD123456D is passed in" in {
        val result = DESConnector.getResidencyStatus(CustomerDetails("DD123456D", "", "", ""))
        result shouldBe InternalServerErrorResponse
      }
    }
  }
}
