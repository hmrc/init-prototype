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

package uk.gov.hmrc.initprototype

import play.api.libs.json.Json

import scalaj.http.{Http, HttpResponse}

object PrototypeKitReleaseUrlResolver {


  type ZipBallUrl = String
  type ErrorMessage = String

  def getLatestZipballUrl(repoApiUrl: String): Either [ErrorMessage , ZipBallUrl] = {
    require(!repoApiUrl.endsWith("/"), s"repository api url should not end '/': $repoApiUrl")

    val latestReleaseUrl = s"$repoApiUrl/releases/latest"
    val response: HttpResponse[String] = Http(latestReleaseUrl).header("content-type" , "application/json").asString

    val responseBody = response.body
    if(response.isNotError) {

      val jsonFieldName = "zipball_url"
      (Json.parse(responseBody) \ jsonFieldName).asOpt[String] match {
        case Some(v) => Right(v)
        case None => Left(s"'$jsonFieldName' is not found in json response: ${Json.prettyPrint(Json.parse(responseBody))}")
      }
    } else {
      Left(s"HTTP error (${response.code}) while getting the release zip artifact from $latestReleaseUrl: $responseBody")
    }
  }
}
