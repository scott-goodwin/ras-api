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

import play.api.Logger
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.rasapi.repository.RasRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DataCleansingService extends DataCleansingService

trait DataCleansingService {

  def removeOrphanedChunks():Future[Seq[BSONObjectID]] = {

    for {
      chunks <- RasRepository.chunksRepo.getAllChunks().map(_.map(_.files_id).distinct)

      fileInfoList <- {       Logger.warn("1 ~~~~~~~~####### chunks to verify #########~~~~~~:-" + chunks.size )

        processFutures(chunks)(RasRepository.filerepo.isFileExists(_))}

      chunksDeleted <- {
        val parentFileIds = fileInfoList.filter(_.isDefined).map(rec => rec.get.id.asInstanceOf[BSONObjectID])
        val chunksToBeDeleted = chunks.diff(parentFileIds)
        Logger.warn("2 ~~~~~~~~####### fileId's to be deleted #########~~~~~~:-"+ chunksToBeDeleted.size )

        val res = processFutures(chunksToBeDeleted)(fileId => {
          Logger.warn("3 ~~~~~###### RAS fileId being deleted  #########~~~~~~" + fileId)
          RasRepository.chunksRepo.removeChunk(fileId)
        })
        Future(chunksToBeDeleted)
      }

    } yield chunksDeleted

  }

  //Further refactor can be done on this
  private def processFutures[A, B](seq: Iterable[A])(fn: A => Future[B])
  : Future[List[B]] =
    seq.foldLeft(Future(List.empty[B])) {
      (previousFuture, next) =>
        for {
          previousResults <- previousFuture
          next <- fn(next)
        } yield previousResults :+ next
    }

}
