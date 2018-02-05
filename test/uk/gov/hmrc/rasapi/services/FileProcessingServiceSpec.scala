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

import java.io.{ByteArrayInputStream, FileInputStream}

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.connectors.{DesConnector, FileUploadConnector}
import uk.gov.hmrc.rasapi.models.{CallbackData, IndividualDetails, ResidencyStatus, ResidencyStatusFailure}
import uk.gov.hmrc.rasapi.repositories.RepositoriesHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FileProcessingServiceSpec extends UnitSpec with OneAppPerSuite with ScalaFutures with MockitoSugar with BeforeAndAfter with RepositoriesHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockFileUploadConnector = mock[FileUploadConnector]

  val mockDesConnector = mock[DesConnector]
  val mockSessionCache = mock[SessionCacheService]

  val SUT = new FileProcessingService {

    override val fileUploadConnector = mockFileUploadConnector
    override val desConnector = mockDesConnector
  }

  val inputStreamfromFile = {
    val resultsArr = Array("LE241131B,Jim,Jimson,1990-02-21",
      "LE241131B,GARY,BRAVO,1990-02-21",
      "LE241131B,SIMON,DAWSON,1990-02-21",
      "LE241131B,MICHEAL,SLATER,1990-02-21"
    )
    val res = await(TestFileWriter.generateFile(resultsArr.iterator))
    new FileInputStream(res.toFile)
  }


  "FileProcessingService" should {
    "readFile" when {
      "file exists line by line" in {

        val envelopeId: String = "0b215e97-11d4-4006-91db-c067e74fc653"
        val fileId: String = "file-id-1"

        val row1 = "John,Smith,AB123456C,1990-02-21".getBytes
        val inputStream = new ByteArrayInputStream(row1)

        when(mockFileUploadConnector.getFile(any(), any())(any())).thenReturn(Future.successful(Some(inputStream)))

        val result = await(SUT.readFile(envelopeId, fileId))

        result.toList should contain theSameElementsAs List("John,Smith,AB123456C,1990-02-21")
      }
    }

    "createMatchingData" when {
      "parse line as raw data and convert to IndividualDetails object" in {
        val inputData = "AB123456C,John,Smith,1995-02-21"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Left(IndividualDetails("AB123456C", "JOHN", "SMITH", new DateTime("1995-02-21")))
      }

      "parse line as raw data and convert to RawMemberDetails object when there are 4 columns with at least one containing empty data" in {
        val inputData = ",Smith,AB123456C,90-02-21"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("nino-MISSING_FIELD", "lastName-INVALID_FORMAT", "dateOfBirth-INVALID_DATE"))
      }

      "parse line as raw data and convert to RawMemberDetails object when there are less than 3 columns" in {
        val inputData = "Smith,AB123456C,1996-02-21"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("nino-INVALID_FORMAT", "lastName-INVALID_FORMAT", "dateOfBirth-MISSING_FIELD", "firstName-INVALID_FORMAT"))
      }

      "parse empty line as raw data and convert to RawMemberDetails object" in {
        val inputData = ",,"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("nino-MISSING_FIELD", "lastName-MISSING_FIELD", "dateOfBirth-MISSING_FIELD", "firstName-MISSING_FIELD"))
      }
    }

    val data = IndividualDetails("AB123456C", "JOHN", "SMITH", new DateTime("1992-02-21"))

    "fetch result" when {
      "input row is valid" in {
        when(mockDesConnector.getResidencyStatus(data)(hc)).thenReturn(
          Future.successful(Left(ResidencyStatus("otherUKResident", "scotResident"))))
        val inputRow = "AB123456C,John,Smith,1992-02-21"
        val result = await(SUT.fetchResult(inputRow))
        result shouldBe "AB123456C,John,Smith,1992-02-21,otherUKResident,scotResident"
      }
      "input row matching failed" in {
        when(mockDesConnector.getResidencyStatus(data)(hc)).thenReturn(
          Future.successful(Right(ResidencyStatusFailure("NOT_MATCHED", "matching failed"))))
        val inputRow = "AB123456C,John,Smith,1992-02-21"
        val result = await(SUT.fetchResult(inputRow))
        result shouldBe "AB123456C,John,Smith,1992-02-21,NOT_MATCHED"
      }
      "input row is inValid" in {
        val inputRow = "456C,John,Smith,1994-02-21"
        val result = await(SUT.fetchResult(inputRow))
        result shouldBe "456C,John,Smith,1994-02-21,nino-INVALID_FORMAT"
      }
    }

    "process file and generate results file " when {
      "valid file is submitted by user" in {

        when(mockFileUploadConnector.getFile(any(), any())(any())).thenReturn(Future.successful(Some(inputStreamfromFile)))
        when(mockFileUploadConnector.deleteUploadedFile(any(), any())(any())).thenReturn(Future.successful(true))

        val expectedResultsFile = "LE241131B,Jim,Jimson,1990-02-21,otherUKResident,scotResident" +
          "LE241131B,GARY,BRAVO,1990-02-21,otherUKResident,scotResident" +
          "LE241131B,SIMON,DAWSON,1990-02-21,otherUKResident,scotResident" +
          "LE241131B,MICHEAL,SLATER,1990-02-21,otherUKResident,scotResident"


        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc653"
        val fileId = "file-id-1"
        val fileStatus = "AVAILABLE"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        when(mockSessionCache.updateFileSession(any(), any(), any())(any()))
          .thenReturn(Future.successful(CacheMap("sessionValue", Map("1234" -> Json.toJson(callbackData)))))

        when(mockDesConnector.getResidencyStatus(any[IndividualDetails])(any())).thenReturn(
          Future.successful(Left(ResidencyStatus("otherUKResident", "scotResident"))))
        await(SUT.processFile("user1234", callbackData))
        Thread.sleep(3000)
        val res = await(rasFileRepository.fetchFile(fileId))
        var result = new String("")
        val temp = await(res.get.data run getAll map { bytes => result = result.concat(new String(bytes)) })
        result.replaceAll("(\\r|\\n)", "") shouldBe expectedResultsFile.mkString

      }
    }
  }
}
