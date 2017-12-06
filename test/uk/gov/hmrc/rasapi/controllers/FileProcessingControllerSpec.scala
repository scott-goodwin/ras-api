package uk.gov.hmrc.rasapi.controllers

import org.mockito.Matchers.{eq => Meq, any}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.models.CallbackData
import uk.gov.hmrc.rasapi.services.FileProcessingService
import play.api.http.Status.OK

class FileProcessingControllerSpec extends UnitSpec with MockitoSugar with OneAppPerSuite with BeforeAndAfter {

  val mockFileProcessingService = mock[FileProcessingService]
  val SUT = new FileProcessingController {
    override val fileProcessingService: FileProcessingService = mockFileProcessingService
  }

  before {
    reset(mockFileProcessingService)
  }

  "statusCallback" should {
    "return Ok and interact with FileProcessingService" when {
      "an 'AVAILABLE' status is given" in {

        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc653"
        val fileId = "file-id-1"
        val fileStatus = "AVAILABLE"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        val result = SUT.statusCallback().apply(FakeRequest(Helpers.POST, "/ras-api/file-processing/status")
          .withJsonBody(Json.toJson(callbackData)))

        verify(mockFileProcessingService).processFile(Meq(envelopeId), Meq(fileId))(any())

        status(result) shouldBe OK
      }
    }

    "return Ok and not interact with FileProcessingServicec" when {
      "an 'ERROR' status is given" in {
        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc653"
        val fileId = "file-id-1"
        val fileStatus = "ERROR"
        val reason: Option[String] = Some("VirusDetected")
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        val result = SUT.statusCallback().apply(FakeRequest(Helpers.POST, "/ras-api/file-processing/status")
          .withJsonBody(Json.toJson(callbackData)))

        verifyZeroInteractions(mockFileProcessingService)

        status(result) shouldBe OK
      }

      "a 'QUARANTINED' status is given" in {
        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc653"
        val fileId = "file-id-1"
        val fileStatus = "QUARANTINED"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        val result = SUT.statusCallback().apply(FakeRequest(Helpers.POST, "/ras-api/file-processing/status")
          .withJsonBody(Json.toJson(callbackData)))

        verifyZeroInteractions(mockFileProcessingService)

        status(result) shouldBe OK
      }

      "a 'CLEANED'status is given" in {
        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc653"
        val fileId = "file-id-1"
        val fileStatus = "CLEANED"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        val result = SUT.statusCallback().apply(FakeRequest(Helpers.POST, "/ras-api/file-processing/status")
          .withJsonBody(Json.toJson(callbackData)))

        verifyZeroInteractions(mockFileProcessingService)

        status(result) shouldBe OK
      }

      "an 'INFECTED' status is given" in {
        val envelopeId = "0b215ey97-11d4-4006-91db-c067e74fc653"
        val fileId = "file-id-1"
        val fileStatus = "INFECTED"
        val reason: Option[String] = None
        val callbackData = CallbackData(envelopeId, fileId, fileStatus, reason)

        val result = SUT.statusCallback().apply(FakeRequest(Helpers.POST, "/ras-api/file-processing/status")
          .withJsonBody(Json.toJson(callbackData)))

        verifyZeroInteractions(mockFileProcessingService)

        status(result) shouldBe OK
      }
    }
  }
}