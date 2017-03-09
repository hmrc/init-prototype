package uk.gov.hmrc.initprototype

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, urlEqualTo}
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.RequestMethod.GET
import org.scalatest.{EitherValues, FunSpec, Matchers}
import play.api.libs.json.Json

class PrototypeKitReleaseUrlResolverSpec extends FunSpec with WireMockEndpoints with Matchers with EitherValues {


  describe("Latest zip url") {
    it("should be correctly extracted from the response json from github") {

      givenGitHubExpects(
        method = GET,
        url = "/releases/latest",
        extraHeaders = Map(
          "content-type" -> "application/json"
        ),
        willRespondWith = (200, Some(
          """{
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
            |}""".stripMargin)))


      PrototypeKitReleaseUrlResolver.getLatestZipballUrl(endpointMockUrl).right.value shouldBe "https://api.github.com/repos/alphagov/govuk_prototype_kit/zipball/v5.1.0"

    }

    it("should produce error message on http error") {

      givenGitHubExpects(
        method = GET,
        url = "/releases/latest",
        extraHeaders = Map("content-type" -> "application/json"),
        willRespondWith = (404, Some("THE ERROR FROM REMOTE")))

      PrototypeKitReleaseUrlResolver.getLatestZipballUrl(endpointMockUrl).left.value shouldBe "HTTP error (404) while getting the release zip artifact from http://localhost:6001/releases/latest: THE ERROR FROM REMOTE"

    }

    it("should produce error message on json parsing error") {

      val jsonReponse =
        """{
         |  "url":"https://api.github.com/repos/alphagov/govuk_prototype_kit/releases/5313431",
         |  "assets_url":"https://api.github.com/repos/alphagov/govuk_prototype_kit/releases/5313431/assets"
         |}""".stripMargin

      givenGitHubExpects(
        method = GET,
        url = "/releases/latest",
        extraHeaders = Map("content-type" -> "application/json"),
        willRespondWith = (200, Some(jsonReponse)))

      PrototypeKitReleaseUrlResolver.getLatestZipballUrl(endpointMockUrl).left.value shouldBe s"'zipball_url' is not found in json response: ${Json.prettyPrint(Json.parse(jsonReponse))}"

    }
  }

  def givenGitHubExpects[T](
                             method: RequestMethod,
                             url: String,
                             extraHeaders: Map[String, String] = Map(),
                             willRespondWith: (Int, Option[String])): Unit = {

    val builder = extraHeaders.foldLeft(new MappingBuilder(method, urlEqualTo(url))) { (acc, header) =>
      acc.withHeader(header._1, equalTo(header._2))
    }


    val response: ResponseDefinitionBuilder = new ResponseDefinitionBuilder()
      .withStatus(willRespondWith._1)

    val resp = willRespondWith._2.map { b =>
      response.withBody(b)
    }.getOrElse(response)

    builder.willReturn(resp)

    endpointMock.register(builder)
  }

}