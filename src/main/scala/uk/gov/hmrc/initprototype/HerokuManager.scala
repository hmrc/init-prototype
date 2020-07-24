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
import play.api.libs.json.{JsObject, JsValue, Json}
import scalaj.http.{Http, HttpResponse}

import scala.concurrent.{ExecutionContext, Future, blocking}

class HerokuManager(implicit config: HerokuConfiguration, ec: ExecutionContext) {
  import config._

  private def requestHeaders = Map(
    "Content-Type"  -> "application/json",
    "Authorization" -> s"Bearer $apiToken",
    "Accept"        -> "application/vnd.heroku+json; version=3"
  )

  private def herokuRequest(
    url: String,
    method: String                         = "GET",
    body: String                           = "",
    additionalHeaders: Map[String, String] = Map.empty) =
    blocking {
      println(s"Starting Heroku request: $url")
      val httpThing = Http(s"$baseUrl$url")
        .timeout(connTimeoutMs = 10000, readTimeoutMs = 50000)
        .headers(requestHeaders ++ additionalHeaders)
      val httpWithBody = if (body != "") {
        httpThing.postData(body).method(method)
      } else {
        httpThing.method(method)
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

  def getApps: Future[Seq[JsObject]] = Future {
    val (responseBody, _, _) = herokuRequest("/apps/")

    Json.parse(responseBody).as[Seq[JsObject]]
  }

  def getAppNames(): Future[Seq[String]] = getApps.map { apps =>
    val appNames: Seq[String] = for (app <- apps) yield (app \ "name").as[String]
    appNames.sorted
  }

  def getAppReleases(app: String, range: Option[String] = None): Future[(Seq[JsObject], Option[String])] = Future {
    val url         = s"/apps/$app/releases/"
    val rangeHeader = Map("Range" -> range.getOrElse(";max=1000;"))

    val (body, _, headers) = herokuRequest(url, "GET", "", rangeHeader)
    val parsed             = Json.parse(body).validate[Seq[JsObject]]
    if (parsed.isError) {
      throw new Exception(s"Error fetching releases for $app with $body")
    }

    val releases                  = parsed.get
    val nextRange: Option[String] = headers.get("next-range").flatMap(_.headOption)

    (releases, nextRange)
  }

  def getAppReleasesRecursive(
    appName: String,
    accumulator: Seq[JsObject] = Seq.empty,
    range: Option[String]      = None): Future[(Seq[JsObject], Option[String])] =
    getAppReleases(appName, range).flatMap { tupleResponse =>
      val (releases: Seq[JsObject], nextRange: Option[String]) = tupleResponse
      val combinedReleases                                     = accumulator ++ releases

      if (nextRange.isEmpty) {
        Future.successful((combinedReleases, nextRange))
      } else {
        getAppReleasesRecursive(appName, combinedReleases, nextRange)
      }
    }

  def getAppQuantityAndSize(appName: String): Future[Option[(Int, String)]] = getAppFormation(appName).map {
    formation =>
      formation.map { formation =>
        val quantity = (formation \ "quantity").as[Int]
        val size     = (formation \ "size").as[String]

        (quantity, size)
      }
  }

  def getAppFormation(app: String): Future[Option[JsObject]] = Future {
    val (body, _, _) = herokuRequest(s"/apps/$app/formation/")

    Json.parse(body).as[Seq[JsObject]].headOption
  }

  def spinDownApp(app: String): Future[JsValue] = Future {
    val url          = s"/apps/$app/formation/web"
    val (body, _, _) = herokuRequest(url, "PATCH", """{"quantity":0,"size":"Standard-1X","type":"web"}""")

    val appFormation = Json.parse(body)

    appFormation
  }
}
