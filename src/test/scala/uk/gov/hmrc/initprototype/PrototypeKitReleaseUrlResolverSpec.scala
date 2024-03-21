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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.RequestMethod.GET
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class PrototypeKitReleaseUrlResolverSpec extends AnyFunSpec with WireMockEndpoints with Matchers with EitherValues {

  private final val releaseURL: String = "/releases/tags/v12.3.0"

  describe("Latest zip url") {
    it("should be correctly extracted from the response json from github") {

      givenGitHubExpects(
        method = GET,
        url = releaseURL,
        extraHeaders = Map(
          "content-type" -> "application/json"
        ),
        willRespondWith = (
          200,
          Some("""{
            |  "url": "https://api.github.com/repos/alphagov/govuk_prototype_kit/releases/5313431",
            |  "assets_url": "https://api.github.com/repos/alphagov/govuk_prototype_kit/releases/5313431/assets",
            |  "upload_url": "https://uploads.github.com/repos/alphagov/govuk_prototype_kit/releases/5313431/assets{?name,label}",
            |  "html_url": "https://github.com/alphagov/govuk_prototype_kit/releases/tag/v5.1.0",
            |  "id": 5313431,
            |  "stuff":"..........",
            |  "prerelease": false,
            |  "tarball_url": "https://api.github.com/repos/alphagov/govuk_prototype_kit/tarball/v5.1.0",
            |  "zipball_url": "https://api.github.com/repos/alphagov/govuk_prototype_kit/zipball/v5.1.0",
            |  "body": "bla"
            |}""".stripMargin),
          Map.empty
        )
      )

      PrototypeKitReleaseUrlResolver
        .getLatestZipballUrl(endpointMockUrl)
        .right
        .value shouldBe "https://api.github.com/repos/alphagov/govuk_prototype_kit/zipball/v5.1.0"

    }

    it("should be correctly extracted if Github requires authorisation") {
      givenGitHubExpects(
        method = GET,
        url = releaseURL,
        extraHeaders = Map(
          "Authorization" -> "token 1111111111"
        ),
        willRespondWith = (
          200,
          Some("""{
            |  "zipball_url": "https://url-to-zip-archive"
            |}""".stripMargin),
          Map.empty
        )
      )

      PrototypeKitReleaseUrlResolver
        .getLatestZipballUrl(endpointMockUrl, Some("1111111111"))
        .right
        .value shouldBe "https://url-to-zip-archive"
    }

    it("should be correctly extracted if Github requires a different authorisation token") {
      givenGitHubExpects(
        method = GET,
        url = releaseURL,
        extraHeaders = Map(
          "Authorization" -> "token 2222222222"
        ),
        willRespondWith = (
          200,
          Some("""{
            |  "zipball_url": "https://url-to-zip-archive"
            |}""".stripMargin),
          Map.empty
        )
      )

      PrototypeKitReleaseUrlResolver
        .getLatestZipballUrl(endpointMockUrl, Some("2222222222"))
        .right
        .value shouldBe "https://url-to-zip-archive"
    }

    it("should throw an error if authorisation fails") {
      val errorFromRemote =
        """{
                              |  "message":"API rate limit exceeded for 0.0.0.0. (But here's the good news: Authenticated requests get a higher rate limit. Check out the documentation for more details.)",
                              |  "documentation_url":"https://developer.github.com/v3/#rate-limiting"
                              |}""".stripMargin
      givenGitHubExpects(
        method = GET,
        url = releaseURL,
        willRespondWith = (403, Some(errorFromRemote), Map.empty)
      )

      PrototypeKitReleaseUrlResolver
        .getLatestZipballUrl(endpointMockUrl)
        .left
        .value shouldBe s"HTTP error (403) while getting the release zip artifact from $endpointMockUrl$releaseURL: $errorFromRemote"
    }

    it("should follow redirects retrieved from github") {

      givenGitHubExpects(
        method = GET,
        url = releaseURL,
        extraHeaders = Map(
          "content-type" -> "application/json"
        ),
        willRespondWith = (
          301,
          Some(s"""{
            |  "message": "Moved Permanently",
            |  "url": "$endpointMockUrl/releases/redirect-target",
            |  "documentation_url": "https://developer.github.com/v3/#http-redirects"
            |}
            |""".stripMargin),
          Map("Location" -> s"$endpointMockUrl/releases/redirect-target")
        )
      )

      givenGitHubExpects(
        method = GET,
        url = "/releases/redirect-target",
        extraHeaders = Map(
          "content-type" -> "application/json"
        ),
        willRespondWith = (
          200,
          Some("""{
            |  "url": "https://api.github.com/repos/alphagov/govuk_prototype_kit/releases/5313431",
            |  "assets_url": "https://api.github.com/repos/alphagov/govuk_prototype_kit/releases/5313431/assets",
            |  "upload_url": "https://uploads.github.com/repos/alphagov/govuk_prototype_kit/releases/5313431/assets{?name,label}",
            |  "html_url": "https://github.com/alphagov/govuk_prototype_kit/releases/tag/v5.1.0",
            |  "id": 5313431,
            |  "stuff":"..........",
            |  "prerelease": false,
            |  "tarball_url": "https://api.github.com/repos/alphagov/govuk_prototype_kit/tarball/v5.1.0",
            |  "zipball_url": "https://api.github.com/repos/alphagov/govuk_prototype_kit/zipball/v5.1.0",
            |  "body": "bla"
            |}""".stripMargin),
          Map.empty
        )
      )

      PrototypeKitReleaseUrlResolver
        .getLatestZipballUrl(endpointMockUrl)
        .right
        .value shouldBe "https://api.github.com/repos/alphagov/govuk_prototype_kit/zipball/v5.1.0"

    }

    it("should follow redirects retrieved from github and pass on any authorisation") {
      givenGitHubExpects(
        method = GET,
        url = releaseURL,
        extraHeaders = Map(
          "Authorization" -> "token abc"
        ),
        willRespondWith = (
          301,
          Some(s"""{
             |  "message": "Moved Permanently",
             |  "url": "$endpointMockUrl/releases/redirect-target",
             |  "documentation_url": "https://developer.github.com/v3/#http-redirects"
             |}
             |""".stripMargin),
          Map("Location" -> s"$endpointMockUrl/releases/redirect-target")
        )
      )

      givenGitHubExpects(
        method = GET,
        url = "/releases/redirect-target",
        extraHeaders = Map(
          "Authorization" -> "token abc"
        ),
        willRespondWith = (
          200,
          Some("""{
            |  "zipball_url": "https://api.github.com/repos/alphagov/govuk_prototype_kit/zipball/v5.1.0"
            |}""".stripMargin),
          Map.empty
        )
      )

      PrototypeKitReleaseUrlResolver
        .getLatestZipballUrl(endpointMockUrl, Some("abc"))
        .right
        .value shouldBe "https://api.github.com/repos/alphagov/govuk_prototype_kit/zipball/v5.1.0"
    }

    it("should produce error message on http error") {

      givenGitHubExpects(
        method = GET,
        url = releaseURL,
        extraHeaders = Map("content-type" -> "application/json"),
        willRespondWith = (404, Some("THE ERROR FROM REMOTE"), Map.empty)
      )

      PrototypeKitReleaseUrlResolver
        .getLatestZipballUrl(endpointMockUrl)
        .left
        .value shouldBe s"HTTP error (404) while getting the release zip artifact from $endpointMockUrl$releaseURL: THE ERROR FROM REMOTE"

    }

    it("should produce error message on json parsing error") {

      val jsonReponse =
        """{
         |  "url":"https://api.github.com/repos/alphagov/govuk_prototype_kit",
         |  "assets_url":"https://api.github.com/repos/alphagov/govuk_prototype_kit"
         |}""".stripMargin

      givenGitHubExpects(
        method = GET,
        url = releaseURL,
        extraHeaders = Map("content-type" -> "application/json"),
        willRespondWith = (200, Some(jsonReponse), Map.empty)
      )

      PrototypeKitReleaseUrlResolver
        .getLatestZipballUrl(endpointMockUrl)
        .left
        .value shouldBe s"'zipball_url' is not found in json response: ${Json.prettyPrint(Json.parse(jsonReponse))}"

    }
  }

  def givenGitHubExpects[T](
    method: RequestMethod,
    url: String,
    extraHeaders: Map[String, String] = Map(),
    willRespondWith: (Int, Option[String], Map[String, String])
  ): Unit = {

    val builder = extraHeaders.foldLeft(request(method.toString, urlEqualTo(url))) { (acc, header) =>
      acc.withHeader(header._1, equalTo(header._2))
    }

    val response: ResponseDefinitionBuilder = new ResponseDefinitionBuilder()
      .withStatus(willRespondWith._1)

    val responseWithHeaders = willRespondWith._3.foldLeft(response) { (acc, header) =>
      acc.withHeader(header._1, header._2)
    }

    val responseWithBody = willRespondWith._2
      .map { b =>
        responseWithHeaders.withBody(b)
      }
      .getOrElse(response)

    builder.willReturn(responseWithBody)

    endpointMock.register(builder)
  }

}
