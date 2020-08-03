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
import play.api.libs.json.Json
import scalaj.http.{Http, HttpResponse}

import scala.concurrent.{ExecutionContext, Future, blocking}

class HerokuManager(implicit config: HerokuConfiguration, ec: ExecutionContext) {
  import config._

  private val requestHeaders = Map(
    "Content-Type"  -> "application/json",
    "Authorization" -> s"Bearer $apiToken",
    "Accept"        -> "application/vnd.heroku+json; version=3"
  )

  private def herokuRequest(
    url: String,
    method: String                         = "GET",
    body: Option[String]                   = None,
    additionalHeaders: Map[String, String] = Map.empty) =
    blocking {
      println(s"Starting Heroku request: $url")

      val http = Http(s"$baseUrl$url")
        .timeout(connTimeoutMs, readTimeoutMs)
        .headers(requestHeaders ++ additionalHeaders)
      val httpWithBody = if (body.isDefined) {
        http.postData(body.get)
      } else {
        http
      }

      val HttpResponse(responseBody, code, headers) = httpWithBody
        .method(method)
        .asString
      if (code < 200 || code > 299) {
        throw new Exception(s"Error with Heroku request $url: $responseBody")
      }

      println(s"Finished Heroku request: $url")

      (responseBody, code, headers)
    }

  def getApps: Future[Seq[HerokuApp]] = Future {
    val (responseBody, _, _) = herokuRequest("/apps/")

    Json.parse(responseBody).as[Seq[HerokuApp]]
  }

  def getAppNames: Future[Seq[String]] = getApps.map { apps =>
    val appNames: Seq[String] = for (app <- apps) yield app.name
    appNames.sorted
  }

  def getAppReleases(app: String, range: Option[String] = None): Future[(Seq[HerokuRelease], Option[String])] = Future {
    val url         = s"/apps/$app/releases/"
    val rangeHeader = Map("Range" -> range.getOrElse(";max=1000;"))

    val (body, _, headers) = herokuRequest(url, additionalHeaders = rangeHeader)
    val parsed             = Json.parse(body).validate[Seq[HerokuRelease]]
    if (parsed.isError) {
      throw new Exception(s"Error fetching releases for $app with $body")
    }

    val releases                  = parsed.get
    val nextRange: Option[String] = headers.get("next-range").flatMap(_.headOption)

    (releases, nextRange)
  }

  def getAppReleasesRecursive(
    appName: String,
    accumulator: Seq[HerokuRelease] = Seq.empty,
    range: Option[String]      = None): Future[(Seq[HerokuRelease], Option[String])] =
    getAppReleases(appName, range).flatMap {
      case (releases: Seq[HerokuRelease], nextRange: Option[String]) =>
        val combinedReleases = accumulator ++ releases

        if (nextRange.isEmpty) {
          Future.successful((combinedReleases, nextRange))
        } else {
          getAppReleasesRecursive(appName, combinedReleases, nextRange)
        }
    }

  def getAppFormation(app: String): Future[Option[HerokuFormation]] = Future {
    val url = s"/apps/$app/formation/"

    val (body, _, _) = herokuRequest(url)

    Json.parse(body).as[Seq[HerokuFormation]].headOption
  }

  def spinDownApp(app: String): Future[HerokuFormation] = Future {
    val url  = s"/apps/$app/formation/web"
    val body = Some("""{"quantity":0,"size":"Standard-1X","type":"web"}""")

    val (responseBody, _, _) = herokuRequest(url, method = "PATCH", body)

    Json.parse(responseBody).as[HerokuFormation]
  }
}
