package uk.gov.hmrc.initprototype

import java.io.File

import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, urlEqualTo}
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.RequestMethod._
import org.apache.commons.io.{FileUtils, IOUtils}
import org.scalatest.FunSpec

import scala.io.Source

class GithubArtifactDownloaderSpec extends FunSpec with WireMockEndpoints {


  describe("getRepoZipAndExplode") {
    it("should download zip from github using correct details") {
      val token = "token123"
      val url = s"$endpointMockUrl/xyz"

      givenGitHubExpects(
        method = GET,
        url = "/xyz",
        extraHeaders = Map(
          "Authorization" -> s"token $token",
          "content-type" -> "application/json"
        ),
        willRespondWithFileContents = (200, Some("test.zip")))

      GithubArtifactDownloader.getRepoZipAndExplode(url, GithubCredentials("user1", token))


    }
  }

  def givenGitHubExpects[T](
                             method: RequestMethod,
                             url: String,
                             extraHeaders: Map[String, String] = Map(),
                             willRespondWithFileContents: (Int, Option[String])): Unit = {

    val builder = extraHeaders.foldLeft(new MappingBuilder(method, urlEqualTo(url))) { (acc, header) =>
      acc.withHeader(header._1, equalTo(header._2))
    }



    val fileContents =
      willRespondWithFileContents._2.map(file => IOUtils.toByteArray(getClass.getClassLoader.getResourceAsStream(file)))


    val response: ResponseDefinitionBuilder = new ResponseDefinitionBuilder()
      .withStatus(willRespondWithFileContents._1)

    val resp = fileContents.map { b =>
      response.withBody(b)
    }.getOrElse(response)

    builder.willReturn(resp)

    endpointMock.register(builder)
  }


}
