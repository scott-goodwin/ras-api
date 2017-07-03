package uk.gov.hmrc.rasapi.models

import play.api.libs.json.Json

case class CustomerCacheResponse(errorResponse: Int, nino: Option[Nino] )

object CustomerCacheResponse{
  implicit val formats = Json.format[CustomerCacheResponse]
}