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

package uk.gov.hmrc.rasapi.repository

import java.io.FileInputStream
import java.nio.file.Path

import org.joda.time.DateTime
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.gridfs.Implicits._
import reactivemongo.api.gridfs._
import reactivemongo.api.{BSONSerializationPack, DB, DBMetaCommands}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.rasapi.models.{FileDetails, ResultsFile}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object RasRepository extends MongoDbConnection{

  private implicit val connection = mongoConnector.db

  lazy val filerepo: RasFileRepository = new RasFileRepository(connection)
}

class RasFileRepository(mongo: () => DB with DBMetaCommands)(implicit ec: ExecutionContext)
  extends ReactiveRepository[FileDetails, BSONObjectID]("filedatastore", mongo, FileDetails.fileFormats, ReactiveMongoFormats.objectIdFormats) {

  private val name = "results.csv"
  private val contentType =  "text/csv"
  private val gridFSG = new GridFS[BSONSerializationPack.type](mongo(), "resultsFiles")
  private val fileToSave = DefaultFileToSave(name, Some(contentType))

  def saveFile(filePath:Path) : Future[ResultsFile] =
  {

    gridFSG.writeFromInputStream(fileToSave,new FileInputStream(filePath.toFile)).map{ res=> logger.warn("File length is "+ res.length);
      ResultsFile(res.id,res.filename.get,res.length,new DateTime(res.uploadDate.get))
    }
      .recover{case ex:Throwable => throw new RuntimeException("failed to upload") }
  }

}