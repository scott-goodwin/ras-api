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

package uk.gov.hmrc.rasapi.repositories

import java.io.ByteArrayInputStream

import play.api.libs.iteratee.{Enumerator, Iteratee}
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.models.ResultsFile
import uk.gov.hmrc.rasapi.repository.RasFileRepository
import uk.gov.hmrc.rasapi.services.RasFileWriter

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

trait RepositoriesHelper extends MongoSpecSupport with UnitSpec {

  private val hostPort = System.getProperty("mongoHostPort", "127.0.0.1:27017")
  override val databaseName = "ras-api"
  private val mongoConnector = MongoConnector(s"mongodb://$hostPort/$databaseName").db

  val rasFileRepository = new RasFileRepositoryTest

  val resultsArr = Array("456C,John,Smith,1990-02-21,nino-INVALID_FORMAT",
    "AB123456C,John,Smith,1990-02-21,NOT_MATCHED",
    "AB123456C,John,Smith,1990-02-21,otherUKResident,scotResident")

  val tempFile = Array("TestMe START",1234345,"sdfjdkljfdklgj", "Test Me END")

  object fileWriter extends RasFileWriter

  lazy val createFile = {
    await(fileWriter.createResultsFile(resultsArr.iterator))
  }

  def saveTempFile() = {
    val filePath = await(fileWriter.createResultsFile(tempFile.iterator))
    rasFileRepository.saveFile("user123","envelope123",filePath, "file123")
  }

  def saveTempFileToRemove() = {
    val filePath = await(fileWriter.createResultsFile(tempFile.iterator))
    rasFileRepository.saveFile("user222","envelope222",filePath, "file222")
  }
  case class FileData( data: Enumerator[Array[Byte]] = null)

  def getAll: Iteratee[Array[Byte], Array[Byte]] = Iteratee.consume[Array[Byte]]()

  class RasFileRepositoryTest(implicit ec: ExecutionContext) extends RasFileRepository(mongoConnector) {

   def getFile( storedFile: ResultsFile) ={
    val inputStream = gridFSG.enumerate(storedFile) run getAll map { bytes =>
        new ByteArrayInputStream(bytes)
      }
      Source.fromInputStream(inputStream).getLines
    }

    def removeAll: Unit = {
      removeFile("file111")
      removeFile("file123")
      removeFile("file222")

    }
  }


}