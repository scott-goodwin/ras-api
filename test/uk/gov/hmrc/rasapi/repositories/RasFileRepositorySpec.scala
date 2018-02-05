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

import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerTest
import play.api.Logger
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

class RasFileRepositorySpec extends UnitSpec with MockitoSugar with OneAppPerTest
  with BeforeAndAfter with RepositoriesHelper {

  before{
    rasFileRepository.removeAll()
  }


"RasFileRepository" should {
  "saveFile" in {
    val file = await(rasFileRepository.saveFile("user111","envelope111",createFile,"file111" ))

    file.filename.get shouldBe "file111"
    val result =  await(rasFileRepository.getFile(file))
      val actual = result.toArray
    Logger.debug(actual.mkString)
      actual shouldBe resultsArr
  }

  "get File" in {
    val resultFile = await(saveTempFile)
    Logger.debug("resultFile.id.toString  -> " + resultFile.id.toString)
    val res = await(rasFileRepository.fetchFile(resultFile.filename.get))
   // res.get.data. shouldBe tempFile
    val result = ListBuffer[String]()
    res.get.data run getAll map {bytes => result += new String(bytes)}
  }


  "removeFile" in {
    val resultFile = await(saveTempFileToRemove)
    Logger.debug(s"file to remove ---> name : ${resultFile.filename.get} id = ${resultFile.id}  " )

        val res = await(rasFileRepository.removeFile(resultFile.filename.get))
        res shouldBe true
        val fileData = await(rasFileRepository.fetchFile(resultFile.filename.get))
        fileData.isDefined shouldBe false
  }
}
}
