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
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.rasapi.connectors.FileUploadConnector
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

trait RasFileReader {
  implicit val system = ActorSystem()
  implicit val materializer:ActorMaterializer = ActorMaterializer()

  val fileUploadConnector: FileUploadConnector

  def readFile(envelopeId: String, fileId: String)(implicit hc: HeaderCarrier): Future[Iterator[String]] = {
    val result = fileUploadConnector.getFile(envelopeId, fileId).map{
      case Some(inputStream) => Source.fromInputStream(inputStream).getLines
      case None => Logger.error("File Processing: problem reading data in the file");throw new FileNotFoundException
    }

    result.map(list => println(s"################################################## [RasFileReader] LIST SIZE: ${list.toList.size}"))

    result
  }
}

trait RasFileWriter {
  def writeFile(): Future[Int]= {
    Future(1)
  }
}