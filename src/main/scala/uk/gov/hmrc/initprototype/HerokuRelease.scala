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

import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue, Reads}

import scala.util.{Success, Try}

case class HerokuRelease(
  version: Int,
  userEmail: String,
  createdAt: String,
  description: String,
  slugId: Option[String]
)

object HerokuRelease {
  implicit def reads: Reads[HerokuRelease] = new Reads[HerokuRelease] {
    override def reads(json: JsValue): JsResult[HerokuRelease] = {
      val maybeAppRelease = Try {
        val version: Int           = (json \ "version").as[Int]
        val userEmail: String      = (json \ "user" \ "email").as[String]
        val slugId: Option[String] = (json \ "slug" \ "id").asOpt[String]
        val createdAt              = (json \ "created_at").as[String]
        val description            = (json \ "description").as[String]
        HerokuRelease(version, userEmail, createdAt, description, slugId)
      }
      maybeAppRelease match {
        case Success(release) => JsSuccess(release)
        case _                => JsError("Could not parse as HerokuAppRelease")
      }
    }
  }
}
