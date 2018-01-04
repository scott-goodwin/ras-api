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

package uk.gov.hmrc.rasapi

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.data.validation.ValidationError
import play.api.libs.json._
import reactivemongo.api.BSONSerializationPack
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson.BSONValue



package object models {

  type NINO = String
  type Name = String
  type ResultsFile = ReadFile[BSONSerializationPack.type, BSONValue]

  object JsonReads {

    private val invalidDataType = "INVALID_DATA_TYPE"
    private val invalidFormat = "INVALID_FORMAT"
    private val missing = "MISSING_FIELD"
    private val invalidDateValidationMessage = "INVALID_DATE"
    private val dateRegex = "^[\\d]{4}-[\\d]{2}-[\\d]{2}$"


    val nino: Reads[NINO] = Reads.pattern(
      "^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$".r,
      invalidFormat)

    val name: Reads[Name] = nameReads()

    // ISO Date is a date which is not in the future.
    def isoDate(dateFormat: String = "yyyy-MM-dd"): Reads[DateTime] = isoDateReads(dateFormat)

    /**
      * Ensures that a name only contains specific characters and is between 1 and 35 characters long,
      * and does not contain solo blank spaces e.g. " ".
      * @return
      */
    private def nameReads(): Reads[Name] = new Reads[Name] {

      def reads(json: JsValue): JsResult[Name] = {

        json match {
          case JsString(data) => data match {
            case strValue if (strValue.trim.isEmpty) => JsError(Seq(JsPath() -> Seq(ValidationError(missing))))
            case strValue if (!strValue.matches("^[a-zA-Z &`\\-\\'^]{1,35}$")) => JsError(Seq(JsPath() -> Seq(ValidationError(invalidFormat))))
            case strValue => JsSuccess(strValue)
          }
          case _ => JsError(Seq(JsPath() -> Seq(ValidationError(invalidDataType))))
        }
      }
    }

    /**
      * Reads a JSON value as a joda.org.time.DateTime object.
      * This date cannot be in the future.
      * @return
      */
    private def isoDateReads(dateFormat: String): Reads[DateTime] = new Reads[DateTime] {

      def reads(json: JsValue): JsResult[DateTime] = json match {
        case JsString(s) => parseDate(s) match {
          case Some(d: DateTime) => {
            if (d.isAfterNow) {
              JsError(Seq(JsPath() -> Seq(ValidationError(invalidDateValidationMessage))))
            }
            else {
              JsSuccess(d)
            }
          }
          case None =>
            s.matches(dateRegex) match {
              case true => JsError(Seq(JsPath() -> Seq(ValidationError(invalidDateValidationMessage))))
              case false => JsError(Seq(JsPath() -> Seq(ValidationError(invalidFormat))))
            }
        }
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError(missing))))
      }

      private def parseDate(input: String): Option[DateTime] =
        scala.util.control.Exception.allCatch[DateTime] opt DateTime.parse(input, DateTimeFormat.forPattern(dateFormat))

    }
  }
}
