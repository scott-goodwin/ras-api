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