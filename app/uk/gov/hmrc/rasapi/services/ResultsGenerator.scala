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
import uk.gov.hmrc.rasapi.models.{IndividualDetails, RawMemberDetails}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait ResultsGenerator {
  val comma = ","

  val desConnector:DesConnector

  def fetchResult(inputRow:String)(implicit hc: HeaderCarrier):Future[String] = {
    createMatchingData(inputRow) match {
      case Right(errors) => Logger.debug("Json errors Exists" + errors.mkString(comma))
        Future(s"${inputRow},${errors.mkString(comma)}")
      case Left(memberDetails) => desConnector.getResidencyStatus(memberDetails).map{ status =>
        status match {
          case Left(residencyStatus) => inputRow + comma +residencyStatus.toString
          case Right(statusFailure) => inputRow + comma +statusFailure.code
        }}.recover {
        case e: Throwable => Logger.error("File processing: Failed getting residency status ")
          throw new RuntimeException
      }
    }
  }

  def createMatchingData(inputRow:String): Either[IndividualDetails,Seq[String]] = {
    val arr = parseString(inputRow)
    Try(Json.toJson(arr).validate[IndividualDetails](IndividualDetails.customerDetailsReads)) match
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

