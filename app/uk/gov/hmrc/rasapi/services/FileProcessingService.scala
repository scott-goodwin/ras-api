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

package uk.gov.hmrc.rasapi.services

import java.io.FileNotFoundException

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import uk.gov.hmrc.rasapi.connectors.{DesConnector, FileUploadConnector}
import uk.gov.hmrc.rasapi.models.{IndividualDetails, RawMemberDetails}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success, Try}

object FileProcessingService extends FileProcessingService {

  override val fileUploadConnector: FileUploadConnector = FileUploadConnector
  override val desConnector:DesConnector = DesConnector
}

trait FileProcessingService {

  implicit val system = ActorSystem()
  implicit val materializer:ActorMaterializer = ActorMaterializer()

  val fileUploadConnector: FileUploadConnector
  val desConnector:DesConnector

  val arr= Array("firstName", "lastName", "nino","dateOfBirth")


  def readFile(envelopeId: String, fileId: String)(implicit hc: HeaderCarrier): Future[Iterator[String]] = {

    fileUploadConnector.getFile(envelopeId, fileId).map{
      case Some(inputStream) => Source.fromInputStream(inputStream).getLines()
      case None => throw new FileNotFoundException()
    }
  }

  def processFile(envelopeId: String, fileId: String)(implicit hc: HeaderCarrier) = {
    val sucessResults:ListBuffer[String] = ListBuffer.empty
    val failurResults:ListBuffer[String] = ListBuffer.empty

    val res = readFile(envelopeId,fileId).map(res =>
     for(row <- res) yield {
       if(!row.isEmpty) fetchResult(row) //failurResults+=s"${row},${errors.mkString(",")}"
      .map(res=> sucessResults+=s"${row},${res.toString}")
     }
    ).recover{case e: Throwable => "06" }

  }

  def fetchResult(inputRow:String)(implicit hc: HeaderCarrier):Future[String] = {
    def getStatus(memberDetails:IndividualDetails) :Future[String]= desConnector.getResidencyStatus(memberDetails).map{ status =>
      status match {
        case Left(residencyStatus) => residencyStatus.toString
        case Right(statusFailure) => statusFailure.code
      }}.recover {
      case e: Throwable => "05"
    }
    createMatchingData(inputRow) match {
      case Right(errors) => Future(s"${inputRow},${errors.mkString(",")}")
      case Left(details) => getStatus(details).map(inputRow + ","+_)
    }

  }


/*  private def inputStreamToString(is: InputStream) = {
    val inputStreamReader = new InputStreamReader(is)
    val bufferedReader = new BufferedReader(inputStreamReader)
    Iterator continually bufferedReader.readLine takeWhile (_ != null) mkString
  }*/

  def createMatchingData(inputRow:String): Either[IndividualDetails,Seq[String]] = {
//    if (inputRow.isEmpty) None
     val arr = parseString(inputRow)
        Try(Json.toJson(arr).validate[IndividualDetails](IndividualDetails.customerDetailsReads)) match
          {
          case Success(JsSuccess(details, _)) => Left(details)
          case Success(JsError(errors)) => Logger.debug(errors.mkString);
            Right(errors.map(err => s"${err._1.toString.substring(1)}-${err._2.head.message}"))
          case Failure(e) => Right(Seq("INVALID RECORD"))
        }
  }

  private def parseString(inputRow: String) = {

    val cols = inputRow.split(",")
    val res = cols ++ (for (x <- 0 until 4- cols.length ) yield "")
    RawMemberDetails(res(0),res(1),res(2),res(3))
//    for { (x,y) <- (arr,res).zipped
//    } yield (x,y)
  }

}

