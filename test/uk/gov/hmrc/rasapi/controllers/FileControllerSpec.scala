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

package uk.gov.hmrc.rasapi.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.libs.iteratee.Enumerator
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.repository.FileData

import scala.concurrent.Future

class FileControllerSpec  extends UnitSpec with MockitoSugar with OneAppPerSuite with BeforeAndAfter {

  val fileData = FileData(length = 124L,Enumerator("TEST START ".getBytes))

  val fileController = new FileController{
    override def getFile(name: String): Future[Option[FileData]] = Some(fileData)
  }

  "FileController" should(
    "serve a file" when {
      "valid filename is provided" in {
        implicit val actorSystem = ActorSystem()
        implicit val materializer = ActorMaterializer()

        val result =  await(fileController.serveFile("testFile.csv").apply(FakeRequest(Helpers.GET, "/ras-api/file/getFile/:testFile")))
        result.header.status shouldBe Status.OK
        val headers = result.header.headers
        headers("Content-Length") shouldBe "124"
        headers("Content-Type") shouldBe "application/octet-stream"
        headers("Content-Disposition") shouldBe "attachment; filename=\"testFile.csv\""

/*            val stream = result.body.dataStream.runWith(StreamConverters.asInputStream())(materializer)
       val fileOutput =  Source.fromInputStream(stream).getLines
        Logger.debug("fileout is " + fileOutput)*/

      }
    }
  )
}
