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

package uk.gov.hmrc.rasapi.services

import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.rasapi.connectors.DesConnector
import uk.gov.hmrc.rasapi.helpers.ResidencyYearResolver
import uk.gov.hmrc.rasapi.models.{IndividualDetails, RawMemberDetails}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait ResultsGenerator {
  val comma = ","

  val desConnector:DesConnector
  val residencyYearResolver: ResidencyYearResolver

  def fetchResult(inputRow:String, userId: String)(implicit hc: HeaderCarrier):String = {
    createMatchingData(inputRow) match {
      case Right(errors) => Logger.debug("Json errors Exists" + errors.mkString(comma))
        s"$inputRow,${errors.mkString(comma)}"
      case Left(memberDetails) =>
        //this needs to be sequential / blocking and at the max 30 TPS
        val res = Await.result(desConnector.getResidencyStatus(memberDetails, userId),20 second)

        res match {
          case Left(residencyStatus) => {
            val resStatus = if (residencyYearResolver.isBetweenJanAndApril()) residencyStatus
                            else residencyStatus.copy(nextYearForecastResidencyStatus = None)
            inputRow + comma + resStatus.toString
          }
          case Right(statusFailure) => inputRow + comma + statusFailure.code.replace("DECEASED", "MATCHING_FAILED")

        }
    }
  }

  def createMatchingData(inputRow:String): Either[IndividualDetails,Seq[String]] = {
    val arr = parseString(inputRow)
    Try(Json.toJson(arr).validate[IndividualDetails](IndividualDetails.individualDetailsReads)) match
    {
      case Success(JsSuccess(details, _)) => Left(details)
      case Success(JsError(errors)) => Logger.debug(errors.mkString)
        Right(errors.map(err => s"${err._1.toString.substring(1)}-${err._2.head.message}"))
      case Failure(e) => Right(Seq("INVALID RECORD"))
    }
  }

  private def parseString(inputRow: String) = {
    val cols = inputRow.split(comma)
    val res = cols ++ (for (x <- 0 until 4- cols.length ) yield "")
    RawMemberDetails(res(0),res(1),res(2),res(3))
  }
}

