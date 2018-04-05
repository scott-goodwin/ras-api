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
import java.nio.file.Files

import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => Meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, RequestTimeoutException}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.connectors.{DesConnector, FileUploadConnector}
import uk.gov.hmrc.rasapi.helpers.ResidencyYearResolver
import uk.gov.hmrc.rasapi.models._
import uk.gov.hmrc.rasapi.repositories.RepositoriesHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Random, Try}

class FileProcessingServiceSpec extends UnitSpec with OneAppPerSuite with ScalaFutures with MockitoSugar with BeforeAndAfter with RepositoriesHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val fakeReq = FakeRequest("POST", "/relief-at-source/customer/residency-status")

  val mockFileUploadConnector = mock[FileUploadConnector]

  val mockDesConnector = mock[DesConnector]
  val mockSessionCache = mock[SessionCacheService]
  val mockResidencyYearResolver = mock[ResidencyYearResolver]
  val mockAuditService = mock[AuditService]

  val SUT = new FileProcessingService {

    override val fileUploadConnector = mockFileUploadConnector
    override val desConnector = mockDesConnector
    override val residencyYearResolver = mockResidencyYearResolver
    override val auditService: AuditService = mockAuditService

    override def getCurrentDate: DateTime = DateTime.now()

    override val allowDefaultRUK: Boolean = false
    override val retryLimit: Int = 3
//    override val waitTime: Long = 1000L
  }

  def getTestFilePath = {
    val successresultsArr = Array("LE241131B,Jim,Jimson,1990-02-21",
      "LE241131B,GARY,BRAVO,1990-02-21",
      "LE241131B,SIMON,DAWSON,1990-02-21",
      "LE241131B,MICHEAL,SLATER,1990-02-21"
    )
    await(TestFileWriter.generateFile(successresultsArr.iterator))
  }

  when(mockDesConnector.otherUk).thenReturn("otherUKResident")
  when(mockDesConnector.scotRes).thenReturn("scotResident")

  before {
    reset(mockFileUploadConnector)
    reset(mockDesConnector)
    reset(mockResidencyYearResolver)
    reset(mockAuditService)
  }

  "fetchResult" should {
    "successfully audit processed data" when {
      "there is a successful result returned when a request is made on 4th Feb 2018" in {

        val testFilePath = getTestFilePath

        val SUT = new FileProcessingService {

          override val fileUploadConnector = mockFileUploadConnector
          override val desConnector = mockDesConnector
          override val residencyYearResolver = mockResidencyYearResolver
          override val auditService: AuditService = mockAuditService

          override def getCurrentDate: DateTime = new DateTime("2018-02-04")

          override val allowDefaultRUK: Boolean = true
          override val retryLimit: Int = 3
//          override val waitTime: Long = 1000L
        }

        when(mockFileUploadConnector.getFile(any(), any())(any())).thenReturn(Future.successful(Some(new FileInputStream(testFilePath.toFile))))
        when(mockFileUploadConnector.deleteUploadedFile(any(), any())(any())).thenReturn(Future.successful(true))

        when(mockDesConnector.otherUk).thenReturn("otherUKResident")
        when(mockDesConnector.scotRes).thenReturn("scotResident")

        val expectedResultsFile = "LE241131B,Jim,Jimson,1990-02-21,otherUKResident,otherUKResident" +
          "LE241131B,GARY,BRAVO,1990-02-21,otherUKResident,otherUKResident" +
          "LE241131B,SIMON,DAWSON,1990-02-21,otherUKResident,otherUKResident" +
          "LE241131B,MICHEAL,SLATER,1990-02-21,otherUKResident,otherUKResident"

        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc651"
        val fileId = Random.nextInt().toString
        val fileStatus = "AVAILABLE"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        when(mockSessionCache.updateFileSession(any(), any(), any())(any()))
          .thenReturn(Future.successful(CacheMap("sessionValue", Map("user1234" -> Json.toJson(callbackData)))))

        when(mockDesConnector.getResidencyStatus(any[IndividualDetails], any())).thenReturn(
          Future.successful(Left(ResidencyStatus("scotResident", Some("otherUKResident")))))

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(true)

        await(SUT.processFile("user1234", callbackData))

        Thread.sleep(20000)

        val res = await(rasFileRepository.fetchFile(fileId))
        var result = new String("")
        val temp = await(res.get.data run getAll map { bytes => result = result.concat(new String(bytes)) })
        result.replaceAll("(\\r|\\n)", "") shouldBe expectedResultsFile.mkString
        Files.deleteIfExists(testFilePath)

        verify(mockAuditService, times(4)).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq(s"/relief-at-source/customer/residency-status"),
          auditData = Meq(Map("successfulLookup" -> "true",
            "CYStatus" -> "otherUKResident",
            "NextCYStatus" -> "otherUKResident",
            "nino" -> "LE241131B",
            "userIdentifier" -> "user1234",
            "requestSource" -> "FE_BULK"))
        )(any())
      }

      "there is a successful result returned when a request is made on 4th Feb 2019" in {

        val testFilePath = getTestFilePath

        val SUT = new FileProcessingService {

          override val fileUploadConnector = mockFileUploadConnector
          override val desConnector = mockDesConnector
          override val residencyYearResolver = mockResidencyYearResolver
          override val auditService: AuditService = mockAuditService

          override def getCurrentDate: DateTime = new DateTime("2019-02-04")

          override val allowDefaultRUK: Boolean = true
          override val retryLimit: Int = 3
//          override val waitTime: Long = 1000L
        }

        when(mockFileUploadConnector.getFile(any(), any())(any())).thenReturn(Future.successful(Some(new FileInputStream(testFilePath.toFile))))
        when(mockFileUploadConnector.deleteUploadedFile(any(), any())(any())).thenReturn(Future.successful(true))

        when(mockDesConnector.otherUk).thenReturn("otherUKResident")
        when(mockDesConnector.scotRes).thenReturn("scotResident")

        val expectedResultsFile = "LE241131B,Jim,Jimson,1990-02-21,scotResident,scotResident" +
          "LE241131B,GARY,BRAVO,1990-02-21,scotResident,scotResident" +
          "LE241131B,SIMON,DAWSON,1990-02-21,scotResident,scotResident" +
          "LE241131B,MICHEAL,SLATER,1990-02-21,scotResident,scotResident"

        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc651"
        val fileId = Random.nextInt().toString
        val fileStatus = "AVAILABLE"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        when(mockSessionCache.updateFileSession(any(), any(), any())(any()))
          .thenReturn(Future.successful(CacheMap("sessionValue", Map("user1234" -> Json.toJson(callbackData)))))

        when(mockDesConnector.getResidencyStatus(any[IndividualDetails], any())).thenReturn(
          Future.successful(Left(ResidencyStatus("scotResident", Some("scotResident")))))

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(true)

        await(SUT.processFile("user1234", callbackData))

        Thread.sleep(20000)

        val res = await(rasFileRepository.fetchFile(fileId))
        var result = new String("")
        val temp = await(res.get.data run getAll map { bytes => result = result.concat(new String(bytes)) })
        result.replaceAll("(\\r|\\n)", "") shouldBe expectedResultsFile.mkString
        Files.deleteIfExists(testFilePath)

        verify(mockAuditService, times(4)).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq(s"/relief-at-source/customer/residency-status"),
          auditData = Meq(Map("successfulLookup" -> "true",
            "CYStatus" -> "scotResident",
            "NextCYStatus" -> "scotResident",
            "nino" -> "LE241131B",
            "userIdentifier" -> "user1234",
            "requestSource" -> "FE_BULK"))
        )(any())
      }

      "there is a successful result returned when a request is made on 4th June 2018" in {

        val testFilePath = getTestFilePath

        val SUT = new FileProcessingService {

          override val fileUploadConnector = mockFileUploadConnector
          override val desConnector = mockDesConnector
          override val residencyYearResolver = mockResidencyYearResolver
          override val auditService: AuditService = mockAuditService

          override def getCurrentDate: DateTime = new DateTime("2018-06-04")

          override val allowDefaultRUK: Boolean = true
          override val retryLimit: Int = 3
//          override val waitTime: Long = 1000L
        }

        when(mockFileUploadConnector.getFile(any(), any())(any())).thenReturn(Future.successful(Some(new FileInputStream(testFilePath.toFile))))
        when(mockFileUploadConnector.deleteUploadedFile(any(), any())(any())).thenReturn(Future.successful(true))

        when(mockDesConnector.otherUk).thenReturn("otherUKResident")
        when(mockDesConnector.scotRes).thenReturn("scotResident")

        val expectedResultsFile = "LE241131B,Jim,Jimson,1990-02-21,otherUKResident" +
          "LE241131B,GARY,BRAVO,1990-02-21,otherUKResident" +
          "LE241131B,SIMON,DAWSON,1990-02-21,otherUKResident" +
          "LE241131B,MICHEAL,SLATER,1990-02-21,otherUKResident"

        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc651"
        val fileId = Random.nextInt().toString
        val fileStatus = "AVAILABLE"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        when(mockSessionCache.updateFileSession(any(), any(), any())(any()))
          .thenReturn(Future.successful(CacheMap("sessionValue", Map("user1234" -> Json.toJson(callbackData)))))

        when(mockDesConnector.getResidencyStatus(any[IndividualDetails], any())).thenReturn(
          Future.successful(Left(ResidencyStatus("otherUKResident", Some("scotResident")))))

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(false)

        await(SUT.processFile("user1234", callbackData))

        Thread.sleep(20000)

        val res = await(rasFileRepository.fetchFile(fileId))
        var result = new String("")
        val temp = await(res.get.data run getAll map { bytes => result = result.concat(new String(bytes)) })
        result.replaceAll("(\\r|\\n)", "") shouldBe expectedResultsFile.mkString
        Files.deleteIfExists(testFilePath)

        verify(mockAuditService, times(4)).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq(s"/relief-at-source/customer/residency-status"),
          auditData = Meq(Map("successfulLookup" -> "true",
            "CYStatus" -> "otherUKResident",
            "nino" -> "LE241131B",
            "userIdentifier" -> "user1234",
            "requestSource" -> "FE_BULK"))
        )(any())
      }

      "there is a matching failed result" in {

        val testFilePath = getTestFilePath

        val SUT = new FileProcessingService {

          override val fileUploadConnector = mockFileUploadConnector
          override val desConnector = mockDesConnector
          override val residencyYearResolver = mockResidencyYearResolver
          override val auditService: AuditService = mockAuditService

          override def getCurrentDate: DateTime = new DateTime("2018-02-04")

          override val allowDefaultRUK: Boolean = true
          override val retryLimit: Int = 3
//          override val waitTime: Long = 1000L
        }

        when(mockFileUploadConnector.getFile(any(), any())(any())).thenReturn(Future.successful(Some(new FileInputStream(testFilePath.toFile))))
        when(mockFileUploadConnector.deleteUploadedFile(any(), any())(any())).thenReturn(Future.successful(true))

        when(mockDesConnector.otherUk).thenReturn("otherUKResident")
        when(mockDesConnector.scotRes).thenReturn("scotResident")

        val expectedResultsFile = "LE241131B,Jim,Jimson,1990-02-21,MATCHING_FAILED" +
          "LE241131B,GARY,BRAVO,1990-02-21,MATCHING_FAILED" +
          "LE241131B,SIMON,DAWSON,1990-02-21,MATCHING_FAILED" +
          "LE241131B,MICHEAL,SLATER,1990-02-21,MATCHING_FAILED"

        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc651"
        val fileId = Random.nextInt().toString
        val fileStatus = "AVAILABLE"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        when(mockSessionCache.updateFileSession(any(), any(), any())(any()))
          .thenReturn(Future.successful(CacheMap("sessionValue", Map("user1234" -> Json.toJson(callbackData)))))

        when(mockDesConnector.getResidencyStatus(any[IndividualDetails], any())).thenReturn(
          Future.successful(Right(ResidencyStatusFailure(code = "MATCHING_FAILED", reason = "MATCHING_FAILED"))))

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(true)

        await(SUT.processFile("user1234", callbackData))

        Thread.sleep(20000)

        val res = await(rasFileRepository.fetchFile(fileId))
        var result = new String("")
        val temp = await(res.get.data run getAll map { bytes => result = result.concat(new String(bytes)) })
        result.replaceAll("(\\r|\\n)", "") shouldBe expectedResultsFile.mkString
        Files.deleteIfExists(testFilePath)

        verify(mockAuditService, times(4)).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq(s"/relief-at-source/customer/residency-status"),
          auditData = Meq(Map("nino" -> "LE241131B",
            "successfulLookup" -> "false",
            "reason" -> "MATCHING_FAILED",
            "userIdentifier" -> "user1234",
            "requestSource" -> "FE_BULK"))
        )(any())
      }

      "there is a deceased result" in {

        val testFilePath = getTestFilePath

        val SUT = new FileProcessingService {

          override val fileUploadConnector = mockFileUploadConnector
          override val desConnector = mockDesConnector
          override val residencyYearResolver = mockResidencyYearResolver
          override val auditService: AuditService = mockAuditService

          override def getCurrentDate: DateTime = new DateTime("2018-02-04")

          override val allowDefaultRUK: Boolean = true
          override val retryLimit: Int = 3
//          override val waitTime: Long = 1000L
        }

        when(mockFileUploadConnector.getFile(any(), any())(any())).thenReturn(Future.successful(Some(new FileInputStream(testFilePath.toFile))))
        when(mockFileUploadConnector.deleteUploadedFile(any(), any())(any())).thenReturn(Future.successful(true))

        when(mockDesConnector.otherUk).thenReturn("otherUKResident")
        when(mockDesConnector.scotRes).thenReturn("scotResident")

        val expectedResultsFile = "LE241131B,Jim,Jimson,1990-02-21,MATCHING_FAILED" +
          "LE241131B,GARY,BRAVO,1990-02-21,MATCHING_FAILED" +
          "LE241131B,SIMON,DAWSON,1990-02-21,MATCHING_FAILED" +
          "LE241131B,MICHEAL,SLATER,1990-02-21,MATCHING_FAILED"

        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc651"
        val fileId = Random.nextInt().toString
        val fileStatus = "AVAILABLE"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        when(mockSessionCache.updateFileSession(any(), any(), any())(any()))
          .thenReturn(Future.successful(CacheMap("sessionValue", Map("user1234" -> Json.toJson(callbackData)))))

        when(mockDesConnector.getResidencyStatus(any[IndividualDetails], any())).thenReturn(
          Future.successful(Right(ResidencyStatusFailure(code = "DECEASED", reason = "Individual is deceased"))))

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(true)

        await(SUT.processFile("user1234", callbackData))

        Thread.sleep(20000)

        val res = await(rasFileRepository.fetchFile(fileId))
        var result = new String("")
        val temp = await(res.get.data run getAll map { bytes => result = result.concat(new String(bytes)) })
        result.replaceAll("(\\r|\\n)", "") shouldBe expectedResultsFile.mkString
        Files.deleteIfExists(testFilePath)

        verify(mockAuditService, times(4)).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq(s"/relief-at-source/customer/residency-status"),
          auditData = Meq(Map("nino" -> "LE241131B",
            "successfulLookup" -> "false",
            "reason" -> "DECEASED",
            "userIdentifier" -> "user1234",
            "requestSource" -> "FE_BULK"))
        )(any())
      }
    }

    "Handle requests" when {
      "it cannot be processed the first time round" in {
        val testFilePath = getTestFilePath

        val SUT = new FileProcessingService {

          override val fileUploadConnector = mockFileUploadConnector
          override val desConnector = mockDesConnector
          override val residencyYearResolver = mockResidencyYearResolver
          override val auditService: AuditService = mockAuditService

          override def getCurrentDate: DateTime = new DateTime("2018-06-04")

          override val allowDefaultRUK: Boolean = true
          override val retryLimit: Int = 3
//          override val waitTime: Long = 1000L
        }

        when(mockFileUploadConnector.getFile(any(), any())(any())).thenReturn(Future.successful(Some(new FileInputStream(testFilePath.toFile))))
        when(mockFileUploadConnector.deleteUploadedFile(any(), any())(any())).thenReturn(Future.successful(true))

        when(mockDesConnector.otherUk).thenReturn("otherUKResident")
        when(mockDesConnector.scotRes).thenReturn("scotResident")

        val expectedResultsFile = "LE241131B,Jim,Jimson,1990-02-21,otherUKResident" +
          "LE241131B,GARY,BRAVO,1990-02-21,otherUKResident" +
          "LE241131B,SIMON,DAWSON,1990-02-21,otherUKResident" +
          "LE241131B,MICHEAL,SLATER,1990-02-21,otherUKResident"

        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc651"
        val fileId = Random.nextInt().toString
        val fileStatus = "AVAILABLE"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        when(mockSessionCache.updateFileSession(any(), any(), any())(any()))
          .thenReturn(Future.successful(CacheMap("sessionValue", Map("user1234" -> Json.toJson(callbackData)))))

        when(mockDesConnector.getResidencyStatus(any[IndividualDetails], any()))
          .thenReturn(Future.failed(new RequestTimeoutException("")))
          .thenReturn(Future.successful(Left(ResidencyStatus("otherUKResident", Some("scotResident")))))

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(false)

        await(SUT.processFile("user1234", callbackData))

        Thread.sleep(20000)

        verify(mockDesConnector, times(5)).getResidencyStatus(any(), any())

        val res = await(rasFileRepository.fetchFile(fileId))
        var result = new String("")
        val temp = await(res.get.data run getAll map { bytes => result = result.concat(new String(bytes)) })
        result.replaceAll("(\\r|\\n)", "") shouldBe expectedResultsFile.mkString
        Files.deleteIfExists(testFilePath)
      }

      "429 (Too Many Requests) has been returned 3 times already" in {
        val testFilePath = getTestFilePath

        val SUT = new FileProcessingService {

          override val fileUploadConnector = mockFileUploadConnector
          override val desConnector = mockDesConnector
          override val residencyYearResolver = mockResidencyYearResolver
          override val auditService: AuditService = mockAuditService

          override def getCurrentDate: DateTime = new DateTime("2018-06-04")

          override val allowDefaultRUK: Boolean = true
          override val retryLimit: Int = 3
//          override val waitTime: Long = 1000L
        }

        when(mockFileUploadConnector.getFile(any(), any())(any())).thenReturn(Future.successful(Some(new FileInputStream(testFilePath.toFile))))
        when(mockFileUploadConnector.deleteUploadedFile(any(), any())(any())).thenReturn(Future.successful(true))

        when(mockDesConnector.otherUk).thenReturn("otherUKResident")
        when(mockDesConnector.scotRes).thenReturn("scotResident")

        val expectedResultsFile = "LE241131B,Jim,Jimson,1990-02-21,INTERNAL_SERVER_ERROR" +
          "LE241131B,GARY,BRAVO,1990-02-21,INTERNAL_SERVER_ERROR" +
          "LE241131B,SIMON,DAWSON,1990-02-21,INTERNAL_SERVER_ERROR" +
          "LE241131B,MICHEAL,SLATER,1990-02-21,INTERNAL_SERVER_ERROR"

        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc651"
        val fileId = Random.nextInt().toString
        val fileStatus = "AVAILABLE"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        when(mockSessionCache.updateFileSession(any(), any(), any())(any()))
          .thenReturn(Future.successful(CacheMap("sessionValue", Map("user1234" -> Json.toJson(callbackData)))))

        when(mockDesConnector.getResidencyStatus(any[IndividualDetails], any()))
          .thenReturn(Future.successful(Right(ResidencyStatusFailure("INTERNAL_SERVER_ERROR", "Internal server error."))))

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(false)

        await(SUT.processFile("user1234", callbackData))

        Thread.sleep(20000)

        val res = await(rasFileRepository.fetchFile(fileId))
        var result = new String("")
        val temp = await(res.get.data run getAll map { bytes => result = result.concat(new String(bytes)) })
        result.replaceAll("(\\r|\\n)", "") shouldBe expectedResultsFile.mkString
        Files.deleteIfExists(testFilePath)
      }
    }
  }

  "FileProcessingService" should {
    "readFile" when {
      "file exists line by line" in {

        val envelopeId: String = "0b215e97-11d4-4006-91db-c067e74fc656"
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

        result shouldBe Right(List("nino-MISSING_FIELD", "lastName-INVALID_FORMAT", "dateOfBirth-INVALID_FORMAT"))
      }

      "parse line as raw data and convert to RawMemberDetails object when there is an invalid date" in {
        val inputData = "LE241131B,Jim,Jimson,1989-02-31"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("dateOfBirth-INVALID_DATE"))
      }

      "parse line as raw data and convert to RawMemberDetails object when there is a caught invalid date" in {
        val inputData = "LE241131B,Jim,Jimson,1111-15-15"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("dateOfBirth-INVALID_DATE"))
      }

      "parse line as raw data and convert to RawMemberDetails object when there is a date not in yyyy-mm-dd format" in {
        val inputData = "LE241131B,Jim,Jimson,89-09-29"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("dateOfBirth-INVALID_FORMAT"))
      }

      "parse line as raw data and convert to RawMemberDetails object when there is a date in yyyy/mm/dd format" in {
        val inputData = "LE241131B,Jim,Jimson,1999/09/29"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("dateOfBirth-INVALID_FORMAT"))
      }

      "parse line as raw data and convert to RawMemberDetails object when there is a date of birth in the future" in {
        val inputData = "LE241131B,Jim,Jimson,2099-01-01"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("dateOfBirth-INVALID_DATE"))
      }

      "parse line as raw data and convert to RawMemberDetails object when there is and invalid date format" in {
        val inputData = "LE241131B,Jim,Jimson,01/01/2017"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("dateOfBirth-INVALID_FORMAT"))
      }

      "parse line as raw data and convert to RawMemberDetails object when there is a missing date" in {
        val inputData = "LE241131B,Jim,Jimson,"

        val result = SUT.createMatchingData(inputData)

        result shouldBe Right(List("dateOfBirth-MISSING_FIELD"))
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
      val userId = "A123456"
      "input row is valid and the date of processing is between january and april" in {
        when(mockDesConnector.getResidencyStatus(data, userId)).thenReturn(
          Future.successful(Left(ResidencyStatus("otherUKResident", Some("scotResident")))))

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(true)

        val inputRow = "AB123456C,John,Smith,1992-02-21"
        val result = await(SUT.fetchResult(inputRow, userId))
        result shouldBe "AB123456C,John,Smith,1992-02-21,otherUKResident,scotResident"
      }

      "input row is valid and the date of processing is between april and december" in {
        when(mockDesConnector.getResidencyStatus(data, userId)).thenReturn(
          Future.successful(Left(ResidencyStatus("otherUKResident", Some("scotResident")))))

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(false)

        val inputRow = "AB123456C,John,Smith,1992-02-21"
        val result = await(SUT.fetchResult(inputRow, userId))
        result shouldBe "AB123456C,John,Smith,1992-02-21,otherUKResident"
      }

      "input row matching failed" in {
        when(mockDesConnector.getResidencyStatus(data, userId)).thenReturn(
          Future.successful(Right(ResidencyStatusFailure("MATCHING_FAILED", "matching failed"))))
        val inputRow = "AB123456C,John,Smith,1992-02-21"
        val result = await(SUT.fetchResult(inputRow, userId))
        result shouldBe "AB123456C,John,Smith,1992-02-21,MATCHING_FAILED"
      }

      "input row returns deceased" in {
        when(mockDesConnector.getResidencyStatus(data, userId)).thenReturn(
          Future.successful(Right(ResidencyStatusFailure("DECEASED", "This person is deceased."))))
        val inputRow = "AB123456C,John,Smith,1992-02-21"
        val result = await(SUT.fetchResult(inputRow, userId))
        result shouldBe "AB123456C,John,Smith,1992-02-21,MATCHING_FAILED"
      }

      "input row is inValid" in {
        val inputRow = "456C,John,Smith,1994-02-21"
        val result = await(SUT.fetchResult(inputRow, userId))
        result shouldBe "456C,John,Smith,1994-02-21,nino-INVALID_FORMAT"
      }
    }

    "process file and generate results file " when {
      "valid file is submitted by user" in {

        val testFilePath = getTestFilePath

        when(mockFileUploadConnector.getFile(any(), any())(any())).thenReturn(Future.successful(Some(new FileInputStream(testFilePath.toFile))))
        when(mockFileUploadConnector.deleteUploadedFile(any(), any())(any())).thenReturn(Future.successful(true))

        val expectedResultsFile = "LE241131B,Jim,Jimson,1990-02-21,otherUKResident,scotResident" +
          "LE241131B,GARY,BRAVO,1990-02-21,otherUKResident,scotResident" +
          "LE241131B,SIMON,DAWSON,1990-02-21,otherUKResident,scotResident" +
          "LE241131B,MICHEAL,SLATER,1990-02-21,otherUKResident,scotResident"


        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc657"
        val fileId = Random.nextInt().toString
        val fileStatus = "AVAILABLE"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        when(mockSessionCache.updateFileSession(any(), any(), any())(any()))
          .thenReturn(Future.successful(CacheMap("sessionValue", Map("user1234" -> Json.toJson(callbackData)))))

        when(mockDesConnector.getResidencyStatus(any[IndividualDetails], any())).thenReturn(
          Future.successful(Left(ResidencyStatus("otherUKResident", Some("scotResident")))))

        when(mockResidencyYearResolver.isBetweenJanAndApril()).thenReturn(true)

        await(SUT.processFile("user1234", callbackData))


        Thread.sleep(20000)

        val res = await(rasFileRepository.fetchFile(fileId))
        var result = new String("")
        val temp = await(res.get.data run getAll map { bytes => result = result.concat(new String(bytes)) })
        result.replaceAll("(\\r|\\n)", "") shouldBe expectedResultsFile.mkString
        Files.deleteIfExists(testFilePath)
      }
    }

    "return status of error" when {
      "there is a problem manipulating the file" in {

        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc657"
        val fileId = Random.nextInt().toString
        val fileStatus = "AVAILABLE"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        await(SUT.manipulateFile(null, "user1234", callbackData, mockSessionCache))

        val captor = ArgumentCaptor.forClass(classOf[CallbackData])
        verify(mockSessionCache, times(1)).updateFileSession(any(), captor.capture, any())(any())

        val resultsFileMetaData = captor.getValue
        resultsFileMetaData.status shouldBe "ERROR"
      }
    }
  }
}
