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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.rasapi.config.RasSessionCache

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RasFileOutputService {

  val sessionCache: SessionCache

  def outputResults(envelopeId: String, results: Future[List[String]])(implicit hc: HeaderCarrier): Unit = {
    println(Console.YELLOW + " ################# [RasFileOutputService] CAME INTO outputResults" + envelopeId + "   " + results)
    println(Console.WHITE)

    results.onSuccess{
      case result => println(Console.YELLOW + s" List size: ${result.size}" + Console.WHITE)
    }

    results.map{ res =>
      println("#######TEST PRINT  [RasFileOutputService]  ~~~~~~~~~~~~~~~~: " + res.head.orElse("LIST WAS EMPTY"))
      for(line <- results) {
        println(Console.YELLOW + " ~~~~~~~~~~~~~~~~~~~~~~ [RasFileOutputService] " + line)
        println(Console.WHITE + " ############################## [RasFileOutputService] DONE PROCESSING")
        sessionCache.cache[List[String]](envelopeId, res)
      }
    }
    sessionCache.cache[String]("12343", "SUCCESS")
  }
}

object RasFileOutputService extends RasFileOutputService {
  override val sessionCache: SessionCache = RasSessionCache
}
