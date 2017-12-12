package uk.gov.hmrc.rasapi.repositories

import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerTest
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.repository.RasFileRepository

import scala.concurrent.ExecutionContext.Implicits.global


class RasFileRepositorySpec extends UnitSpec with MockitoSugar with OneAppPerTest with BeforeAndAfter with RepositoriesHelper {

  val rasFileRepository = new RasFileRepository(mongoConnector)

"RasFileRepository" should {
  "saveFile" in {
    val file = await(rasFileRepository.saveFile(createFile))

    file.fileName shouldBe "results.csv"

  }
}
}
