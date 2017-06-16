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

import uk.gov.hmrc.rasapi.models.{CustomerDetails, ResidencyStatus}

trait DESConnector {

  def getResidencyStatus(customerDetails: CustomerDetails): Option[ResidencyStatus] = {

    customerDetails match {
      case CustomerDetails("AA123456A", "First Name", "Last Name", "1989-09-29") => Some(ResidencyStatus("otherUKResident","otherUKResident"))
      case CustomerDetails("BB123456B", "John", "Smith", "1975-05-25") => Some(ResidencyStatus("otherUKResident","scotResident"))
      case CustomerDetails("CC123456C", "Jane", "Doe", "1969-06-09") => Some(ResidencyStatus("scotResident","otherUKResident"))
      case CustomerDetails("EE123456E", "Joe", "Bloggs", "1982-02-17") => Some(ResidencyStatus("scotResident","scotResident"))
      case _ => None
    }
  }

}

object DESConnector extends DESConnector
