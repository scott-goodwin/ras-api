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
import scala.util.Try

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

  type FILE_INFO = (Path,BufferedWriter)

  def createFileWriter() : FILE_INFO = {
    val file = Try(Files.createTempFile("results",".csv"))
    file.isSuccess match {
      case true =>  (file.get, new BufferedWriter(new FileWriter(file.get.toFile)))
      case false => Logger.error("Error creating temp file for writing results"); throw new FileNotFoundException
    }
  }

  def writeResultToFile(writer:BufferedWriter, line:String): Boolean = {
    try {
       writer.write(line.toString)
        writer.newLine
      }
    catch {
      case ex: Throwable => Logger.error("Exception in writing line to the results file" + ex.getMessage)
        throw new RuntimeException("Exception in writing line to the results file" + ex.getMessage)
    }
    true
  }

  def closeWriter(writer:BufferedWriter):Boolean ={
    val res = Try(writer.close())
     res.recover{
       case ex:Throwable => Logger.error("Failed to close the Outputstream with error" + ex.getMessage)
         false
     }
    true
  }

  def clearFile(path:Path) :Unit =  if (!Files.deleteIfExists(path)) Logger.error(s"error deleting file or file ${path} doesn't exist")
}
