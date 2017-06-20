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
import uk.gov.hmrc.rasapi.models.{CustomerDetails, ResidencyStatus}

class DESConnectorSpec extends WordSpec with OneAppPerSuite with ShouldMatchers{

  //TODO: The tests below will need to be updated after the migration of test data to stubs

  "DESConnector" should {
    "return correct residency status" when {

      "customer with nino: AA123456A is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("AA123456A", "First Name", "Last Name", "1989-09-29")).map { result =>
          result shouldBe ResidencyStatus("otherUKResident","otherUKResident")
        }
      }

      "customer with nino: BB123456B is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("BB123456B", "John", "Smith", "1975-05-25")).map { result =>
          result shouldBe ResidencyStatus("otherUKResident","scotResident")
        }
      }

      "customer with nino: CC123456C is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("CC123456C", "Jane", "Doe", "1969-06-09")).map { result =>
          result shouldBe ResidencyStatus("scotResident", "otherUKResident")
        }
      }

      "customer with nino: EE123456E is passed in" in {

        DESConnector.getResidencyStatus(CustomerDetails("EE123456E", "Joe", "Bloggs", "1982-02-17")).map { result =>
          result shouldBe ResidencyStatus("scotResident", "scotResident")
        }
      }

      "customer with nino: AB234567B is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("AB234567B", "Peter", "Armstrong", "1969-01-01")).map { result =>
          result shouldBe ResidencyStatus("otherUKResident","otherUKResident")
        }
      }

      "customer with nino: BC234567C is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("BC234567C", "Steven", "Smith", "1947-08-15")).map { result =>
          result shouldBe ResidencyStatus("otherUKResident","scotResident")
        }
      }

      "customer with nino: CD234567D is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("CD234567D", "Simon", "Handyside", "1984-10-31")).map { result =>
          result shouldBe ResidencyStatus("scotResident", "otherUKResident")
        }
      }

      "customer with nino: EF234567F is passed in" in {

        DESConnector.getResidencyStatus(CustomerDetails("EF234567F", "Linda", "Marshall", "1966-06-21")).map { result =>
          result shouldBe ResidencyStatus("scotResident", "scotResident")
        }
      }

      "customer with nino: AC345678C is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("AC345678C", "Kelly", "Thompson", "1990-02-15")).map { result =>
          result shouldBe ResidencyStatus("otherUKResident","otherUKResident")
        }
      }

      "customer with nino: BD345678D is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("BD345678D", "Simon", "Handyside", "1984-10-31") ).map { result =>
          result shouldBe ResidencyStatus("otherUKResident","scotResident")
        }
      }

      "customer with nino: CE345678E is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("CE345678E", "Zack", "Jackson", "1966-04-04")).map { result =>
          result shouldBe ResidencyStatus("scotResident", "otherUKResident")
        }
      }

      "customer with nino: EG345678G is passed in" in {

        DESConnector.getResidencyStatus(CustomerDetails("EG345678G", "Linda", "Marshall", "1966-06-21")).map { result =>
          result shouldBe ResidencyStatus("scotResident", "scotResident")
        }
      }

      "customer with nino: AD456789D is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("AD456789D", "Oscar", "Smith", "1986-06-14")).map { result =>
          result shouldBe ResidencyStatus("otherUKResident","otherUKResident")
        }
      }

      "customer with nino: BE456789E is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("BE456789E", "Louise", "Oscar", "1966-04-04")).map { result =>
          result shouldBe ResidencyStatus("otherUKResident","scotResident")
        }
      }

      "customer with nino: CF456789F is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("CF456789F", "Raj", "Patel", "1984-10-31")).map { result =>
          result shouldBe ResidencyStatus("scotResident", "otherUKResident")
        }
      }

      "customer with nino: EH456789H is passed in" in {

        DESConnector.getResidencyStatus(CustomerDetails("EH456789H", "Mary", "Brown", "1982-02-17")).map { result =>
          result shouldBe ResidencyStatus("scotResident", "scotResident")
        }
      }

      "customer with nino: DD123456D is passed in" in {
        DESConnector.getResidencyStatus(CustomerDetails("DD123456D", "", "", "")).map { result =>
          result shouldBe None
        }
      }
    }
  }

}
