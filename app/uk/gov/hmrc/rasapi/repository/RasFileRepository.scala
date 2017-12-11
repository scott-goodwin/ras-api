package uk.gov.hmrc.rasapi.repository

import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.{JsValue, Json}
import play.modules.reactivemongo.JSONFileToSave
import reactivemongo.api.gridfs.{DefaultFileToSave, GridFS, ReadFile}
import reactivemongo.api.{DB, DBMetaCommands}
import reactivemongo.json.JSONSerializationPack

import scala.concurrent.{ExecutionContext, Future}


class RasFileRepository(mongo: () => DB with DBMetaCommands)(implicit ec: ExecutionContext) {

  type JSONGridFS = GridFS[JSONSerializationPack.type]
  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsValue]
  type ByteStream = Array[Byte]

  lazy val gfs: JSONGridFS = GridFS[JSONSerializationPack.type](mongo(), "rasFiles")

  def iterateeForUpload( envelopeId: String, fileRefId: String)
                       (implicit ec: ExecutionContext): Iteratee[ByteStream, Future[JSONReadFile]] = {

   gfs.iteratee(JSONFileToSave(id = Json.toJson(fileRefId), filename = None, metadata = Json.obj("envelopeId" -> envelopeId, "fileId" -> fileRefId)))
  }


val name = "archive.zip"
  val contentType =  "text/csv"
  val gridFS = new GridFS(mongo(), "rasResults")
  val fileToSave = DefaultFileToSave("archive.zip", Some(contentType))

/*  def futureResult(data: Enumerator[Array[Byte]]) =
  {
    gridFS.save(data,fileToSave)
    gridFS.writeFromInputStream(fileToSave,new FileInputStream(new File(name)))

  }*/
def insertAnyFile() = {
  val text = "I only exists to be stored in mongo :<"
  val contents = Enumerator[ByteStream](text.getBytes)

  val envelopeId = ""
  val fileId = ""
  val fileRefId = ""

  val sink = iterateeForUpload(envelopeId, fileRefId)
  contents.run[Future[JSONReadFile]](sink)
}

}