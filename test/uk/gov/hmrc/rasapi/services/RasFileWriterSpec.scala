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

import java.nio.file.Files

import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.play.test.UnitSpec

import scala.io.Source

class RasFileWriterSpec extends UnitSpec with OneServerPerSuite with ScalaFutures with MockitoSugar with BeforeAndAfter {
  object fileWriter extends RasFileWriter
  val resultsArr = Array("456C,John,Smith,1990-02-21,nino-INVALID_FORMAT",
    "AB123456C,John,Smith,1990-02-21,NOT_MATCHED",
    "AB123456C,John,Smith,1990-02-21,otherUKResident,scotResident")

  "RasFileWriter" should {
    "write results to a file"  in {
      val res = await(fileWriter.createResultsFile(resultsArr.iterator))

      val lines = Source.fromFile(res.toFile).getLines.toArray

      lines should contain theSameElementsAs resultsArr
      lines.size shouldBe 3
      Files.deleteIfExists(res)
    }
  }

}
