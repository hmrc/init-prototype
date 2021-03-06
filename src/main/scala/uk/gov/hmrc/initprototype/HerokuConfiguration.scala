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

package uk.gov.hmrc.initprototype

import com.typesafe.config.{Config, ConfigFactory}
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.collection.JavaConverters._

class HerokuConfiguration {
  private val config: Config           = ConfigFactory.load()
  val baseUrl: String                  = config.getString("heroku.baseUrl")
  val apiToken: String                 = config.getString("heroku.apiToken")
  val timeout: Duration                = Duration(config.getInt("heroku.timeoutMs"), MILLISECONDS)
  val connTimeoutMs: Int               = config.getInt("heroku.connTimeoutMs")
  val readTimeoutMs: Int               = config.getInt("heroku.readTimeoutMs")
  val administratorEmails: Seq[String] = config.getStringList("heroku.administratorEmails").asScala
}
