/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.rasapi.config

import javax.inject.Inject
import play.api.{Configuration, Environment}
import play.api.Mode.Mode
import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig

class AppContext @Inject()(val runModeConfiguration: Configuration, val environment: Environment) extends ServicesConfig {
  lazy val mode: Mode = environment.mode
  lazy val appName = runModeConfiguration.getString("appName").getOrElse(throw new RuntimeException("appName is not configured"))
  lazy val appUrl = runModeConfiguration.getString("appUrl").getOrElse(throw new RuntimeException("appUrl is not configured"))
  lazy val apiContext = runModeConfiguration.getString(s"api.context").getOrElse(throw new RuntimeException(s"Missing Key api.context"))
  lazy val baseUrl = runModeConfiguration.getString("baseUrl").getOrElse(throw new RuntimeException("Missing Key baseUrl"))
  lazy val apiStatus = runModeConfiguration.getString("api.status").getOrElse(throw new RuntimeException(s"Missing Key api.status"))
  lazy val endpointsEnabled = runModeConfiguration.getBoolean("api.endpointsEnabled").getOrElse(throw new RuntimeException(s"Missing key api.endpointsEnabled"))
  lazy val desAuthToken = runModeConfiguration.getString("desauthtoken").getOrElse(throw new RuntimeException(s"Missing Key desauthtoken"))
  lazy val desUrlHeaderEnv: String =  runModeConfiguration.getString("environment").getOrElse(throw new RuntimeException(s"Missing Key environment"))
  lazy val edhUrl: String = runModeConfiguration.getString("endpoints.edh.url").getOrElse(throw new RuntimeException(s"Missing Key edhUrl"))
  lazy val resultsExpriyTime: Long = runModeConfiguration.getLong("results.expiry.time").getOrElse(259200)
  lazy val allowNoNextYearStatus: Boolean = runModeConfiguration.getBoolean("toggle-feature.allow-no-next-year-status").getOrElse(false)
  lazy val allowDefaultRUK: Boolean = runModeConfiguration.getBoolean("toggle-feature.allow-default-ruk").getOrElse(false)
  lazy val retryEnabled: Boolean = runModeConfiguration.getBoolean("toggle-feature.retry-enabled").getOrElse(false)
  lazy val bulkRetryEnabled: Boolean = runModeConfiguration.getBoolean("toggle-feature.bulk-retry-enabled").getOrElse(false)
  lazy val requestRetryLimit: Int = runModeConfiguration.getInt("request-retry-limit").getOrElse(3)
  lazy val retryDelay: Int = runModeConfiguration.getInt("retry-delay").getOrElse(500)
  lazy val deceasedStatus: String = runModeConfiguration.getString("status.deceased").getOrElse("DECEASED")
  lazy val tooManyRequestsStatus: String = runModeConfiguration.getString("status.too-many-requests").getOrElse("TOO_MANY_REQUESTS")
  lazy val matchingFailedStatus: String = runModeConfiguration.getString("status.matching-failed.api").getOrElse("STATUS_UNAVAILABLE")
  lazy val serviceUnavailableStatus: String = runModeConfiguration.getString("status.service-unavailable").getOrElse("SERVICE_UNAVAILABLE")
  lazy val doNotReProcessStatus: String = runModeConfiguration.getString("status.do-not-re-process").getOrElse("DO_NOT_RE_PROCESS")
  lazy val fileProcessingMatchingFailedStatus: String = runModeConfiguration.getString("status.matching-failed.csv").getOrElse("cannot_provide_status")
  lazy val fileProcessingInternalServerErrorStatus: String = runModeConfiguration.getString("status.internal-server-error.csv").getOrElse("problem-getting-status")
  lazy val internalServerErrorStatus: String = runModeConfiguration.getString("status.internal-server-error.api").getOrElse("INTERNAL_SERVER_ERROR")
  lazy val removeChunksDataExerciseEnabled: Boolean = runModeConfiguration.getBoolean("remove-chunks-data-exercise.enabled").getOrElse(false)
  lazy val apiV2_0Enabled: Boolean = runModeConfiguration.getBoolean("api-v2_0.enabled").getOrElse(false)
}
