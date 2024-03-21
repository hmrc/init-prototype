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
import play.api.libs.json.{JsSuccess, Json}
import scalaj.http.{Http, HttpResponse}

import scala.concurrent.{ExecutionContext, Future, blocking}

class HerokuConnector(config: HerokuConfiguration)(implicit ec: ExecutionContext) {
  import config._

  private val requestHeaders = Map(
    "Content-Type"  -> "application/json",
    "Authorization" -> s"Bearer $apiToken",
    "Accept"        -> "application/vnd.heroku+json; version=3"
  )

  private val DEFAULT_RANGE_HEADER = ";max=1000;"

  private def herokuRequest(
    url: String,
    method: String = "GET",
    body: Option[String] = None,
    additionalHeaders: Map[String, String] = Map.empty
  ): Future[(String, Int, Map[String, IndexedSeq[String]])] =
    Future {
      blocking {
        println(s"Starting Heroku request: $url")

        val http         = Http(s"$baseUrl$url")
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
    }

  private def getApps(
    accumulator: Seq[HerokuApp] = Seq.empty,
    range: Option[String] = None
  ): Future[Seq[HerokuApp]] =
    getAppsFromRange(range) flatMap { case (apps, maybeNextRange) =>
      val combinedApps = accumulator ++ apps
      maybeNextRange match {
        case None            => Future(combinedApps)
        case Some(nextRange) => getApps(accumulator = combinedApps, Some(nextRange))
      }
    }

  private def getAppsFromRange(range: Option[String]): Future[(Seq[HerokuApp], Option[String])] = {
    val rangeHeader = Map("Range" -> range.getOrElse(DEFAULT_RANGE_HEADER))
    herokuRequest("/apps/", additionalHeaders = rangeHeader) map { case (body, _, headers) =>
      val apps      = Json.parse(body).as[Seq[HerokuApp]]
      val nextRange = headers.get("next-range").flatMap(_.headOption)
      (apps, nextRange)
    }
  }

  def getAppNames: Future[Seq[String]] = getApps().map(_.map(_.name).sorted)

  def getAppReleasesFromRange(app: String, range: Option[String]): Future[(Seq[HerokuRelease], Option[String])] = {
    val url         = s"/apps/$app/releases/"
    val rangeHeader = Map("Range" -> range.getOrElse(DEFAULT_RANGE_HEADER))

    herokuRequest(url, additionalHeaders = rangeHeader) flatMap { case (body, _, headers) =>
      Json.parse(body).validate[Seq[HerokuRelease]] match {
        case JsSuccess(releases, _) =>
          val nextRange: Option[String] = headers.get("next-range").flatMap(_.headOption)

          Future.successful((releases, nextRange))
        case _                      =>
          Future.failed(throw new Exception(s"Error fetching releases for $app with $body"))
      }
    }
  }

  def getAppReleases(
    appName: String,
    accumulator: Seq[HerokuRelease] = Seq.empty,
    range: Option[String]
  ): Future[(Seq[HerokuRelease], Option[String])] =
    getAppReleasesFromRange(appName, range).flatMap { case (releases: Seq[HerokuRelease], nextRange: Option[String]) =>
      val combinedReleases = accumulator ++ releases

      if (nextRange.isEmpty) {
        Future.successful((combinedReleases, nextRange))
      } else {
        getAppReleases(appName, combinedReleases, nextRange)
      }
    }

  def getAppFormation(app: String): Future[Option[HerokuFormation]] = {
    val url = s"/apps/$app/formation/"

    herokuRequest(url) map { case (body, _, _) =>
      Json.parse(body).as[Seq[HerokuFormation]].headOption
    }
  }

  def spinDownApp(app: String): Future[HerokuFormation] = {
    val url  = s"/apps/$app/formation/web"
    val body = Some("""{"quantity":0,"size":"Standard-1X","type":"web"}""")

    herokuRequest(url, method = "PATCH", body) map { case (responseBody, _, _) =>
      Json.parse(responseBody).as[HerokuFormation]
    }
  }

  def getSlugIds(appName: String): Future[Seq[String]] =
    getAppReleases(appName, range = None) map { case (releases, _) =>
      val orderedByVersion = releases.sortBy(_.version)
      orderedByVersion.flatMap(_.slugId).take(10)
    }

  def getCommitId(appName: String, slugId: String): Future[Option[String]] =
    herokuRequest(url = s"/apps/$appName/slugs/$slugId") map { response =>
      val asJson = Json.parse(response._1)
      (asJson \ "commit").asOpt[String]
    }
}
