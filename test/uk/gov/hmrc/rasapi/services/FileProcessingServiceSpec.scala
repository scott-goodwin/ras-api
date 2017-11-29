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

import java.io.ByteArrayInputStream

import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.ws.{DefaultWSResponseHeaders, StreamedResponse}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.connectors.{DesConnector, FileUploadConnector}
import uk.gov.hmrc.rasapi.models.{IndividualDetails, ResidencyStatus, ResidencyStatusFailure}

import scala.concurrent.Future

class FileProcessingServiceSpec extends UnitSpec with OneServerPerSuite with ScalaFutures with MockitoSugar with BeforeAndAfter {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockFileUploadConnector = mock[FileUploadConnector]

  val mockDesConnector = mock[DesConnector]


  val SUT = new FileProcessingService {

    override val fileUploadConnector = mockFileUploadConnector
    override val desConnector = mockDesConnector
  }

  "FileProcessingService" should {
    "readFile" when {
      "file exists line by line" in {

        val envelopeId: String = "0b215e97-11d4-4006-91db-c067e74fc653"
        val fileId: String = "file-id-1"

        val row1 = "John,Smith,AB123456C,1990-02-21".getBytes
        val inputStream = new ByteArrayInputStream(row1)

        val streamResponse: StreamedResponse = StreamedResponse(DefaultWSResponseHeaders(200, Map("CONTENT_TYPE" -> Seq("application/octet-stream"))),
          Source.apply[ByteString](List(ByteString("John, "), ByteString("Smith, "),
            ByteString("AB123456C, "), ByteString("1990-02-21"))))

        when(mockFileUploadConnector.getFile(any(), any())(any())).thenReturn(Future.successful(Some(inputStream)))

        val result = await(SUT.readFile(envelopeId, fileId))

        result.toList should contain theSameElementsAs List("John,Smith,AB123456C,1990-02-21")
      }
    }

    "createMatchingData" when {
      "parse line as raw data and convert to IndividualDetails object" in {
        val inputData = "AB123456C,John,Smith,1990-02-21"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Left(IndividualDetails("AB123456C","JOHN", "SMITH", new DateTime("1990-02-21")))
      }

      "parse line as raw data and convert to RawMemberDetails object when there are 4 columns with at least one containing empty data" in {
        val inputData = ",Smith,AB123456C,1990-02-21"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("nino-INVALID_FORMAT", "lastName-INVALID_FORMAT"))
      }

      "parse line as raw data and convert to RawMemberDetails object when there are less than 3 columns" in {
        val inputData = "Smith,AB123456C,1990-02-21"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("nino-INVALID_FORMAT", "lastName-INVALID_FORMAT", "dateOfBirth-INVALID_FORMAT", "firstName-INVALID_FORMAT"))
      }

      "parse empty line as raw data and convert to RawMemberDetails object" in {
        val inputData = ",,"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("nino-INVALID_FORMAT", "lastName-MISSING_FIELD", "dateOfBirth-INVALID_FORMAT", "firstName-MISSING_FIELD"))
      }
    }

    val data = IndividualDetails("AB123456C","JOHN", "SMITH", new DateTime("1990-02-21"))

    "fetch result" when {
      "input row is valid" in {
        when(mockDesConnector.getResidencyStatus(data)(hc)).thenReturn(
          Future.successful(Left(ResidencyStatus("otherUKResident","scotResident"))))
        val inputRow = "AB123456C,John,Smith,1990-02-21"
        val result = await(SUT.fetchResult(inputRow))
        result shouldBe "AB123456C,John,Smith,1990-02-21,otherUKResident,scotResident"
      }
      "input row matching failed" in {
        when(mockDesConnector.getResidencyStatus(data)(hc)).thenReturn(
          Future.successful(Right(ResidencyStatusFailure("NOT_MATCHED","matching failed"))))
        val inputRow = "AB123456C,John,Smith,1990-02-21"
        val result = await(SUT.fetchResult(inputRow))
        result shouldBe "AB123456C,John,Smith,1990-02-21,NOT_MATCHED"
      }
      "input row is inValid" in {
        val inputRow = "456C,John,Smith,1990-02-21"
        val result = await(SUT.fetchResult(inputRow))
        result shouldBe "456C,John,Smith,1990-02-21,nino-INVALID_FORMAT"
      }
    }
  }
}
