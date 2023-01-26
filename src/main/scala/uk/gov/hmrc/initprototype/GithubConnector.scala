/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json._
import scalaj.http.{Http, HttpResponse}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, blocking}

class GithubConnector(config: GithubConfiguration)(implicit ec: ExecutionContext) {

  import config._

  private val requestHeaders = Map(
    "Content-Type"  -> "application/json",
    "Accept"        -> "application/vnd.github.text-match+json",
    "Authorization" -> s"token $apiToken"
  )

  def getGithubRepoName(commitId: String): Option[String] = {
    respectGithubApiSearchRateLimit()
    val (githubCommitsResponse, _, _) =
      Await.result(githubRequest(s"search/commits?q=$commitId"), Duration.Inf)
    val commitsAsJson                 = Json.parse(githubCommitsResponse)
    val commitsSearchResults          = (commitsAsJson \ "items").as[Seq[JsObject]]

    commitsSearchResults.headOption match {
      case Some(commitSearchResult) => Some((commitSearchResult \ "repository" \ "name").as[String])
      case None                     => None
    }
  }

  private def respectGithubApiSearchRateLimit(): Unit = {
    val justOverAMinuteInMillis                       = 61000
    val maximumUnauthenticatedGithubSearchesPerMinute = 10
    val timeToSleepInMillis                           = justOverAMinuteInMillis / maximumUnauthenticatedGithubSearchesPerMinute
    Thread.sleep(timeToSleepInMillis)
  }

  def fileExists(path: String, repo: String, owner: String = "hmrc"): Boolean =
    Await.result(githubRequest(s"repos/$owner/$repo/contents/$path"), Duration.Inf) match {
      case (_, 200, _) => true
      case _           => false
    }

  def repoExists(repo: String, owner: String = "hmrc"): Boolean =
    Await.result(githubRequest(s"repos/$owner/$repo"), Duration.Inf) match {
      case (_, 200, _) => true
      case _           => false
    }

  private def githubRequest(
    queryString: String
  ): Future[(String, Int, Map[String, IndexedSeq[String]])] =
    Future {
      blocking {
        val http = Http(s"$baseUrl/$queryString")
          .timeout(connTimeoutMs, readTimeoutMs)
          .headers(requestHeaders)

        println(s"Starting Github request for ${http.url}")

        val HttpResponse(responseBody, code, headers) = http
          .method("GET")
          .asString
        if (code < 200 || (code > 299 && code != 404)) {
          throw new Exception(s"Error with Github API request $queryString: $responseBody")
        }

        println(s"Finished Github API request: ${http.url}")

        (responseBody, code, headers)
      }
    }

}
