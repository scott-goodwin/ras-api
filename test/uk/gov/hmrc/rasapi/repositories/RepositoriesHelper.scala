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

package uk.gov.hmrc.rasapi.repositories

import java.io.ByteArrayInputStream
import java.nio.file.Files

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.iteratee.{Enumerator, Iteratee}
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.models.ResultsFile
import uk.gov.hmrc.rasapi.repository.RasFileRepository
import uk.gov.hmrc.rasapi.services.RasFileWriter

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

trait RepositoriesHelper extends MongoSpecSupport with UnitSpec {

  private val hostPort = System.getProperty("mongoHostPort", "127.0.0.1:27017")
  override val databaseName = "rasFileStore"
  private val mongoConnector = MongoConnector(s"mongodb://$hostPort/$databaseName").db

  val rasFileRepository = new RasFileRepositoryTest

  val resultsArr = Array("456C,John,Smith,1990-02-21,nino-INVALID_FORMAT",
    "AB123456C,John,Smith,1990-02-21,NOT_MATCHED",
    "AB123456C,John,Smith,1990-02-21,otherUKResident,scotResident")

  object fileWriter extends RasFileWriter

  lazy val createFile = {

    await(fileWriter.createResultsFile(resultsArr.iterator))

  }
  case class FileData( data: Enumerator[Array[Byte]] = null)

  class RasFileRepositoryTest(implicit ec: ExecutionContext) extends RasFileRepository(mongoConnector) with ScalaFutures{

    lazy val now = DateTime.now.withZone(DateTimeZone.UTC)
    val resultRows:ListBuffer[String] = new ListBuffer()
    def getFile( storedFile: ResultsFile) ={
      val file = Files.createTempFile("results",".csv")
      def getAll: Iteratee[Array[Byte], Array[Byte]] = Iteratee.consume[Array[Byte]]()



    val inputStream = gridFSG.enumerate(storedFile) run getAll map { bytes =>
        new ByteArrayInputStream(bytes)
      }

      Source.fromInputStream(inputStream).getLines

/*      gridFSG.find(BSONDocument("_id" -> id.toString)).headOption map {
        file => file.map( f => FileData( gridFSG.enumerate(f)).data.map( row =>   new String(row)).run(Iteratee.consume[String]()).futureValue)
      }*/
    }
  }
}