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

import uk.gov.hmrc.rasapi.connectors.{DesConnector, FileUploadConnector}
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FileProcessingService extends FileProcessingService {

  override val fileUploadConnector: FileUploadConnector = FileUploadConnector
  override val desConnector:DesConnector = DesConnector
}

trait FileProcessingService extends RasFileReader with RasFileWriter with ResultsGenerator{

  def processFile(envelopeId: String, fileId: String)(implicit hc: HeaderCarrier): Future[List[String]] = {
    lazy val results:ListBuffer[String] = ListBuffer.empty

    println(Console.YELLOW + "[FileProcessingService] processFile" + Console.WHITE)

//    val iterator1 = readFile(envelopeId,fileId)
//
//    for{
//      iterator2 <- iterator1
//    } yield {
//      while (iterator2.hasNext) {
//        for {
//          result <- fetchResult(iterator2.next())
//        } yield {
//          results += result
//          println(Console.YELLOW + "nkjsdhkjsd: " + result + Console.WHITE)
//        }
//      }
//      results.toList
//    }

    //    readFile(envelopeId,fileId).map { res =>
    //      for (row <- res) yield {
    //        println(Console.YELLOW + s"row: ${row}" + Console.WHITE)
    //        if (!row.isEmpty) fetchResult(row).map(results += _)
//      }
//      println(Console.YELLOW + s"------------------Results size: ${results.size}" + Console.WHITE)
//      results.toList
//    }

    val iterator1 = readFile(envelopeId,fileId)

    for{
      iterator2 <- iterator1
    } yield {
      for (line <- iterator2) {
        val futureResult: Future[String] = fetchResult(line)

        for {
          res <- futureResult
        } yield {
          results += res
        }
      }

      results.toList
    }

/*    val res = results.toList

    println(Console.YELLOW + s" LIST SIZE: ${res.size}")
    println(Console.WHITE)

    res*/
  }

}

