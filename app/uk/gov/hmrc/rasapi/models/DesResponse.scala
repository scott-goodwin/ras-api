package uk.gov.hmrc.rasapi.models

import play.api.libs.json.Json

case class DesResponse(status: Int , residencyStatus: Option[ResidencyStatus])

object DesResponse{
  implicit val formats = Json.format[DesResponse]
}
