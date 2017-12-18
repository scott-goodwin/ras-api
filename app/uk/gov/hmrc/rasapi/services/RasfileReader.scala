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

import java.io._
import java.nio.file.{Files, Path}

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
    fileUploadConnector.getFile(envelopeId, fileId).map{
      case Some(inputStream) => Source.fromInputStream(inputStream).getLines
      case None => Logger.error("File Processing: problem reading data in the file");throw new FileNotFoundException
    }
  }
}

trait RasFileWriter {
  def createResultsFile1(futureIterator: Iterator[Any]) :Path = {

    generateFile(futureIterator)
  }


  def createResultsFile(futureIterator: Future[Iterator[Any]]) :Future[Path] = {

    futureIterator.map{res => generateFile(res)}
  }

   def generateFile(data: Iterator[Any]) :Path = {
     val file = Files.createTempFile("results",".csv")
     val outputStream = new BufferedWriter(new FileWriter(file.toFile))
    try {
      data.foreach { line => outputStream.write(line.toString)
        outputStream.newLine
      }
      file
      //  FILE IS CREATED TEMPORARILY NEED TO SORT THIS OUT
    }
    catch {
      case ex: Throwable => Logger.error("Error creating file" + ex.getMessage)
        outputStream.close ;throw new RuntimeException("Exception in generating file" + ex.getMessage)
    }
    finally outputStream.close
  }

  def clearFile(path:Path)  =  if (Files.deleteIfExists(path) == false) Logger.error(s"error deleting file or file ${path} doesn't exist")
}
