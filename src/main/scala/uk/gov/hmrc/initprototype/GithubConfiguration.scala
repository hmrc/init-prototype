/*
 * Copyright 2024 HM Revenue & Customs
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

class GithubConfiguration {
  private val config: Config = ConfigFactory.load()
  val baseUrl: String        = config.getString("github.baseUrl")
  val apiToken: String       = config.getString("github.apiToken")

  val connTimeoutMs: Int = config.getInt("github.connTimeoutMs")
  val readTimeoutMs: Int = config.getInt("github.readTimeoutMs")

}
