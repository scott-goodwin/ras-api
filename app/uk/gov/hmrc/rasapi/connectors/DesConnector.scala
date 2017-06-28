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

import uk.gov.hmrc.rasapi.models._

trait DESConnector {

  def getResidencyStatus(customerDetails: CustomerDetails): NPSResponse = {

    //TODO: Update this function to call ras-stubs to access the data below

    customerDetails match {
      case CustomerDetails("LE241131B", "Jim", "Jimson", "1989-09-29") => SuccessfulNPSResponse(ResidencyStatus("otherUKResident","otherUKResident"))
      case CustomerDetails("BB123456B", "John", "Smith", "1975-05-25") => SuccessfulNPSResponse(ResidencyStatus("otherUKResident","scotResident"))
      case CustomerDetails("LR325154D", "Jane", "Doe", "1969-06-09") => SuccessfulNPSResponse(ResidencyStatus("scotResident","otherUKResident"))
      case CustomerDetails("CC123456C", "Joe", "Bloggs", "1982-02-17") => SuccessfulNPSResponse(ResidencyStatus("scotResident","scotResident"))
      case CustomerDetails("PC243122B", "Peter", "Armstrong", "1969-01-01") => SuccessfulNPSResponse(ResidencyStatus("otherUKResident","otherUKResident"))
      case CustomerDetails("EE123456D", "Steven", "Smith", "1947-08-15") => SuccessfulNPSResponse(ResidencyStatus("otherUKResident","scotResident"))
      case CustomerDetails("ZR132134C", "Simon", "Handyside", "1984-10-31") => SuccessfulNPSResponse(ResidencyStatus("scotResident","otherUKResident"))
      case CustomerDetails("SG123456D", "Linda", "Marshall", "1966-06-21") => SuccessfulNPSResponse(ResidencyStatus("scotResident","scotResident"))
      case CustomerDetails("CK355335C", "Kelly", "Thompson", "1990-02-15") => SuccessfulNPSResponse(ResidencyStatus("otherUKResident","otherUKResident"))
      case CustomerDetails("AR355335C", "Simon", "Handyside", "1984-10-31") => SuccessfulNPSResponse(ResidencyStatus("otherUKResident","scotResident"))
      case CustomerDetails("NW424252D", "Zack", "Jackson", "1966-04-04") => SuccessfulNPSResponse(ResidencyStatus("scotResident","otherUKResident"))
      case CustomerDetails("KA122234B", "Linda", "Marshall", "1966-06-21") => SuccessfulNPSResponse(ResidencyStatus("scotResident","scotResident"))
      case CustomerDetails("WK332122D", "Oscar", "Smith", "1986-06-14") => SuccessfulNPSResponse(ResidencyStatus("otherUKResident","otherUKResident"))
      case CustomerDetails("RW215443D", "Louise", "Oscar", "1966-04-04") => SuccessfulNPSResponse(ResidencyStatus("otherUKResident","scotResident"))
      case CustomerDetails("SE235112A", "Raj", "Patel", "1984-10-31") => SuccessfulNPSResponse(ResidencyStatus("scotResident","otherUKResident"))
      case CustomerDetails("AE325433D", "Mary", "Brown", "1982-02-17") => AccountLockedResponse
      case _ => InternalServerErrorResponse
    }
  }

}

object DESConnector extends DESConnector
