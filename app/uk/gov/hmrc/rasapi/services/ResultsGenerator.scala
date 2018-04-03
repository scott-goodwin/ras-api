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

import org.joda.time.DateTime
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.rasapi.connectors.DesConnector
import uk.gov.hmrc.rasapi.helpers.ResidencyYearResolver
import uk.gov.hmrc.rasapi.models.{IndividualDetails, RawMemberDetails, ResidencyStatus}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait ResultsGenerator {
  val comma = ","

  val desConnector:DesConnector
  val residencyYearResolver: ResidencyYearResolver
  val auditService: AuditService

  def getCurrentDate: DateTime
  val allowDefaultRUK: Boolean

  val TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS"
  val DECEASED = "DECEASED"
  val MATCHING_FAILED = "MATCHING_FAILED"
  val INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR"
  val REQUEST_TIMED_OUT = "REQUEST_TIMEOUT"
  val BAD_REQUEST = "BAD_REQUEST"

  val retryLimit = 3
  val _1Second = 1000L

  def fetchResult(inputRow:String, userId: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]):String = {

    def getResultAndProcess(memberDetails: IndividualDetails, retryCount:Int = 1): String = {

      val res = Await.result(desConnector.getResidencyStatus(memberDetails, userId),20 second)

      res match {
        case Left(residencyStatus) =>
          val resStatus = if (residencyYearResolver.isBetweenJanAndApril()) updateResidencyResponse(residencyStatus)
          else residencyStatus.copy(nextYearForecastResidencyStatus = None)
          auditResponse(failureReason = None, nino = Some(memberDetails.nino),
            residencyStatus = Some(resStatus), userId = userId)
          inputRow + comma + resStatus.toString

        case Right(statusFailure) =>
          if (statusFailure.code != DECEASED && statusFailure.code != MATCHING_FAILED && retryCount <= retryLimit) {
            Thread.sleep(_1Second)
            getResultAndProcess(memberDetails, retryCount = retryCount + 1)
          } else {
            auditResponse(failureReason = Some(statusFailure.code), nino = Some(memberDetails.nino),
              residencyStatus = None, userId = userId)
            inputRow + comma + statusFailure.code.replace(DECEASED, MATCHING_FAILED).replace(TOO_MANY_REQUESTS, INTERNAL_SERVER_ERROR)
          }
      }
    }

    createMatchingData(inputRow) match {
      case Right(errors) => s"$inputRow,${errors.mkString(comma)}"
      case Left(memberDetails) =>
        //this needs to be sequential / blocking and at the max 30 TPS
        getResultAndProcess(memberDetails)
    }
  }

  def createMatchingData(inputRow:String): Either[IndividualDetails,Seq[String]] = {
    val arr = parseString(inputRow)
    Try(Json.toJson(arr).validate[IndividualDetails](IndividualDetails.individualDetailsReads)) match
    {
      case Success(JsSuccess(details, _)) => Left(details)
      case Success(JsError(errors)) => Right(errors.map(err => s"${err._1.toString.substring(1)}-${err._2.head.message}"))
      case Failure(e) => Right(Seq("INVALID RECORD"))
    }
  }

  private def parseString(inputRow: String) = {
    val cols = inputRow.split(comma)
    val res = cols ++ (for (x <- 0 until 4- cols.length ) yield "")
    RawMemberDetails(res(0),res(1),res(2),res(3))
  }

  private def updateResidencyResponse(residencyStatus: ResidencyStatus): ResidencyStatus = {

    if (getCurrentDate.isBefore(new DateTime(2018, 4, 6, 0, 0, 0, 0)) && allowDefaultRUK)
      residencyStatus.copy(currentYearResidencyStatus = desConnector.otherUk)
    else
      residencyStatus
  }

  /**
    * Audits the response, if failure reason is None then residencyStatus is Some (sucess) and vice versa (failure).
    * @param failureReason Optional message, present if the journey failed, else not
    * @param nino Optional user identifier, present if the customer-matching-cache call was a success, else not
    * @param residencyStatus Optional status object returned from the HoD, present if the journey succeeded, else not
    * @param userId Identifies the user which made the request
    * @param request Object containing request made by the user
    * @param hc Headers
    */
  private def auditResponse(failureReason: Option[String], nino: Option[String],
                            residencyStatus: Option[ResidencyStatus], userId: String)
                           (implicit request: Request[AnyContent], hc: HeaderCarrier): Unit = {

    val ninoMap: Map[String, String] = nino.map(nino => Map("nino" -> nino)).getOrElse(Map())
    val nextYearStatusMap: Map[String, String] = if (residencyStatus.nonEmpty) residencyStatus.get.nextYearForecastResidencyStatus
                                                    .map(nextYear => Map("NextCYStatus" -> nextYear)).getOrElse(Map())
                                                 else Map()
    val auditDataMap: Map[String, String] = failureReason.map(reason => Map("successfulLookup" -> "false",
                                                                            "reason" -> reason))
                                                                            .getOrElse(Map(
                                                                              "successfulLookup" -> "true",
                                                                              "CYStatus" -> residencyStatus.get.currentYearResidencyStatus
                                                                            ) ++ nextYearStatusMap)

    auditService.audit(auditType = "ReliefAtSourceResidency",
      path = request.path,
      auditData = auditDataMap ++ Map("userIdentifier" -> userId, "requestSource" -> "FE_BULK") ++ ninoMap
    )
  }
}
