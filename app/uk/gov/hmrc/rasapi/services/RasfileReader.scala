package uk.gov.hmrc.rasapi.services

import java.io.FileNotFoundException

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.rasapi.connectors.FileUploadConnector

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
  def writeFile(): Future[Int]= {
    Future(1)
  }
}