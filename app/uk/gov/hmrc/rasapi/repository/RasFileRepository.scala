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

/*  type JSONGridFS = GridFS[JSONSerializationPack.type]
  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsValue]
  type ByteStream = Array[Byte]

  lazy val gfs: JSONGridFS = GridFS[JSONSerializationPack.type](mongo(), "rasFiles")

  def uploadStream( envelopeId: String, fileRefId: String)
                       (implicit ec: ExecutionContext): Iteratee[ByteStream, Future[JSONReadFile]] = {

   gfs.iteratee(JSONFileToSave(id = Json.toJson(fileRefId), filename = None, metadata = Json.obj("envelopeId" -> envelopeId, "fileId" -> fileRefId)))
  }*/



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


/*def insertAnyFile() = {
  val text = "I only exists to be stored in mongo :<"
  val contents = Enumerator[ByteStream](text.getBytes)

  val envelopeId = ""
  val fileId = ""
  val fileRefId = ""

  val sink = iterateeForUpload(envelopeId, fileRefId)
  contents.run[Future[JSONReadFile]](sink)
}*/

}