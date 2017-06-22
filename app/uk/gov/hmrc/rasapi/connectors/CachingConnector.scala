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

import uk.gov.hmrc.rasapi.models.CustomerDetails

trait CachingConnector {

  def getCachedData(uuid: String): Option[CustomerDetails] = {

    //TODO: Update this function to call our caching service(to be created) to access the data below

    uuid match {
      case "2800a7ab-fe20-42ca-98d7-c33f4133cfc2" => Some(CustomerDetails("LE241131B", "Jim", "Jimson", "1989-09-29"))
      case "633e0ee7-315b-49e6-baed-d79c3dffe467" => Some(CustomerDetails("BB123456B", "John", "Smith", "1975-05-25"))
      case "77648d82-309e-484d-a310-d0ffd2997791" => Some(CustomerDetails("LR325154D", "Jane", "Doe", "1969-06-09"))
      case "79f21755-8cd4-4785-9c10-13253f7a8bb6" => Some(CustomerDetails("CC123456C", "Joe", "Bloggs", "1982-02-17"))
      case "2900a7ab-fe20-42ca-98d8-c33f4133cfc2" => Some(CustomerDetails("PC243122B", "Peter", "Armstrong", "1969-01-01"))
      case "743e0ee7-315b-49e7-baed-d79c3dffe467" => Some(CustomerDetails("EE123456D", "Steven", "Smith", "1947-08-15"))
      case "88648d82-309e-484e-a310-d0ffd2997791" => Some(CustomerDetails("ZR132134C", "Simon", "Handyside", "1984-10-31"))
      case "88648d82-309e-484d-a310-d0ffd2997792" => Some(CustomerDetails("SG123456D", "Linda", "Marshall", "1966-06-21"))
      case "3000a7ab-fe20-42ca-98d9-c33f4133cfc2" => Some(CustomerDetails("CK355335C", "Kelly", "Thompson", "1990-02-15"))
      case "853e0ee7-315b-49e8-baed-d79c3dffe467" => Some(CustomerDetails("AR355335C", "Simon", "Handyside", "1984-10-31"))
      case "99648d82-309e-484f-a310-d0ffd2997791" => Some(CustomerDetails("NW424252D", "Zack", "Jackson", "1966-04-04"))
      case "99648d82-309e-484d-a310-d0ffd2997793" => Some(CustomerDetails("KA122234B", "Linda", "Marshall", "1966-06-21"))
      case "3100a7ab-fe20-42ca-98d1-c33f4133cfc2" => Some(CustomerDetails("WK332122D", "Oscar", "Smith", "1986-06-14"))
      case "963e0ee7-315b-49e-baed-d79c3dffe467"  => Some(CustomerDetails("RW215443D", "Louise", "Oscar", "1966-04-04"))
      case "11648d82-309e-484g-a310-d0ffd2997791" => Some(CustomerDetails("SE235112A", "Raj", "Patel", "1984-10-31"))
      case "76648d82-309e-484d-a310-d0ffd2997794" => Some(CustomerDetails("AE325433D", "Mary", "Brown", "1982-02-17"))
      case "76648d82-309e-484d-a310-d0ffd2997795" => Some(CustomerDetails("SG325433C", "Mary", "Poppins", "1952-02-17"))
      case _ => None
    }
  }
}

object CachingConnector extends CachingConnector
