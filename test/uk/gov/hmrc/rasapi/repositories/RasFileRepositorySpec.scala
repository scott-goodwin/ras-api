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

import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerTest
import play.api.Logger
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

class RasFileRepositorySpec extends UnitSpec with MockitoSugar with OneAppPerTest
  with BeforeAndAfter with RepositoriesHelper {


"RasFileRepository" should {
  "saveFile" in {
    val file = await(rasFileRepository.saveFile(createFile,"file-1234" ))

    file.filename.get shouldBe "file-1234.csv"
    val result =  await(rasFileRepository.getFile(file))
      val actual = result.toArray
    Logger.debug(actual.mkString)
      actual shouldBe resultsArr
  }
  val resultFile = await(saveTempFile)

  "get File" in {
    val res = await(rasFileRepository.fetchFile(resultFile.filename.get))
   // res.get.data. shouldBe tempFile
    val result = ListBuffer[String]()
    res.get.data run getAll map {bytes => result += new String(bytes)}
  }
  "removeFile" in {
    Logger.debug(s"file to remove ---> name : ${resultFile.filename.get} id = ${resultFile.id}  " )

    val res = await(rasFileRepository.removeFile(resultFile.id))
    res shouldBe false
    val fileData = await(rasFileRepository.fetchFile(resultFile.filename.get))
    Logger.debug("trying to fetch after removal --> " + fileData.get._id)

    fileData shouldBe None
  }
}
}
