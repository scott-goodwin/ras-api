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

package uk.gov.hmrc.rasapi.models

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class RawMemberDetails(nino: String = "",firstName: String = "", lastName: String = "",  dateOfBirth: String = "")

object RawMemberDetails {
  implicit val formats = Json.format[RawMemberDetails]
}

case class IndividualDetails(nino: NINO, firstName: Name, lastName: Name, dateOfBirth: DateTime)

object IndividualDetails {
  implicit val individualDetailsReads: Reads[IndividualDetails] = (
    (JsPath \ "nino").read[NINO](JsonReads.nino) and
      (JsPath \ "firstName").read[Name](JsonReads.name).map(name => name.toUpperCase) and
      (JsPath \ "lastName").read[Name](JsonReads.name).map(name => name.toUpperCase) and
      (JsPath \ "dateOfBirth").read[DateTime](JsonReads.isoDate("yyyy-MM-dd")).map(new DateTime(_))
    )(IndividualDetails.apply _)

  implicit val individualDetailssWrites: Writes[IndividualDetails] = (
    (JsPath \ "nino").write[String] and
      (JsPath \ "firstName").write[String] and
      (JsPath \ "lastName").write[String] and
      (JsPath \ "dateOfBirth").write[String].contramap[DateTime](date => date.toString("yyyy-MM-dd"))
    )(unlift(IndividualDetails.unapply))
}



