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

///*
// * Copyright 2018 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.rasapi.services
//
//import play.api.Logger
//import play.api.libs.json.JsSuccess
//import play.api.mvc.{AnyContent, Request}
//import uk.gov.hmrc.rasapi.connectors.DesConnector
//import uk.gov.hmrc.rasapi.models. _
//
//import scala.concurrent.Future
//import scala.util.{ Success, Try}
//import scala.concurrent.ExecutionContext.Implicits.global
//import uk.gov.hmrc.http.HeaderCarrier
//
////TODO: Remove file!
//trait HttpResponseHandlerService {
//
//  val desConnector: DesConnector
//  val auditService: AuditService
//
//  def handleResidencyStatusResponse(individualDetails: IndividualDetails, userId: String)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Either[ResidencyStatus, String]] = {
//
//    desConnector.getResidencyStatus(individualDetails, userId).map { response =>
//      Try(response.json.validate[ResidencyStatusSuccess]) match {
//        case Success(JsSuccess(payload, _)) =>
//          val resStatus = transformResidencyStatusValues(ResidencyStatus(currentYearResidencyStatus = payload.currentYearResidencyStatus,
//            nextYearForecastResidencyStatus = payload.nextYearResidencyStatus))
//          desConnector.sendDataToEDH(userId, individualDetails.nino, resStatus).map { httpResponse =>
//            auditEDHResponse(userId, individualDetails.nino, auditSuccess = true)
//          } recover {
//            case _ =>
//              Logger.error(s"HttpResponseHandlerService - handleResidencyStatusResponse: Error returned from EDH")
//              auditEDHResponse(userId, individualDetails.nino, auditSuccess = false)
//          }
//          Left(resStatus)
//        case _ =>
//          Logger.error(s"HttpResponseHandlerService - handleResidencyStatusResponse: Error (${response.status}) returned from Des for residency status")
//          Right("")
//      }
//    }
//  }
//
//  private def transformResidencyStatusValues(residencyStatus: ResidencyStatus) = {
//
//    def transformResidencyStatusValue(residencyStatus: String): String = {
//      residencyStatus match {
//        case "Uk" => "otherUKResident"
//        case "Scottish" => "scotResident"
//      }
//    }
//
//    ResidencyStatus(transformResidencyStatusValue(residencyStatus.currentYearResidencyStatus),
//      transformResidencyStatusValue(residencyStatus.nextYearForecastResidencyStatus))
//  }
//
//
//  private def auditEDHResponse(userId: String, nino: String, auditSuccess: Boolean)
//                              (implicit request: Request[AnyContent], hc: HeaderCarrier): Unit = {
//
//    val auditDataMap = Map("userId" -> userId,
//      "nino" -> nino,
//      "edhAuditSuccess" -> auditSuccess.toString)
//
//    auditService.audit(auditType = "ReliefAtSourceAudit",
//      path = request.path,
//      auditData = auditDataMap
//    )
//  }
//}
//
//object HttpResponseHandlerService extends HttpResponseHandlerService {
//
//  override val desConnector = DesConnector
//  override val auditService = AuditService
//}
