package uk.gov.hmrc.rasapi.repositories

import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.services.RasFileWriter

trait RepositoriesHelper extends MongoSpecSupport with UnitSpec {

  val hostPort = System.getProperty("mongoHostPort", "127.0.0.1:27017")
  override val databaseName = "rasFileStore"
  val mongoConnector = MongoConnector(s"mongodb://$hostPort/$databaseName").db


  object fileWriter extends RasFileWriter

  lazy val createFile = {
    val resultsArr = Array("456C,John,Smith,1990-02-21,nino-INVALID_FORMAT",
      "AB123456C,John,Smith,1990-02-21,NOT_MATCHED",
      "AB123456C,John,Smith,1990-02-21,otherUKResident,scotResident")
    await(fileWriter.createResultsFile(resultsArr.iterator))

  }
}