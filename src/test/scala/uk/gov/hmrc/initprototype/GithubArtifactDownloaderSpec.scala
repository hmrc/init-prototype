package uk.gov.hmrc.initprototype

import java.io.{File, FileInputStream}

import ammonite.ops._
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, urlEqualTo}
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.RequestMethod._
import org.apache.commons.io.{FileUtils, IOUtils}
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import org.zeroturnaround.zip.ZipUtil

class GithubArtifactDownloaderSpec extends FunSpec with WireMockEndpoints with Matchers {

  type FilePath = String
  private val tempDirectoryPath = FileUtils.getTempDirectory
  val githubArtifactDownloader = new GithubArtifactDownloader(tempDirectoryPath.toPath.resolve("some-archive.zip").toString)

  
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
        willRespondWithFileContents = (200, Some(zipContentsOfDir(getClass.getClassLoader.getResource("test-dir").getPath))))

      val explodedPath = githubArtifactDownloader.getRepoZipAndExplode(url, GithubCredentials("user1", token))

      explodedPath shouldBe tempDirectoryPath.toPath.resolve("some-archive.zip/foo").toString
      val lsResult = %%('ls)(Path(explodedPath))
      lsResult.out.string should startWith("bar.js")
    }
  }

  def givenGitHubExpects[T](
                             method: RequestMethod,
                             url: String,
                             extraHeaders: Map[String, String] = Map(),
                             willRespondWithFileContents: (Int, Option[FilePath])): Unit = {

    val builder = extraHeaders.foldLeft(new MappingBuilder(method, urlEqualTo(url))) { (acc, header) =>
      acc.withHeader(header._1, equalTo(header._2))
    }

    val fileContents =
      willRespondWithFileContents._2.map(file => IOUtils.toByteArray(new FileInputStream(file)))

    val response: ResponseDefinitionBuilder = new ResponseDefinitionBuilder()
      .withStatus(willRespondWithFileContents._1)

    val resp = fileContents.map { b =>
      response.withBody(b)
    }.getOrElse(response)

    builder.willReturn(resp)

    endpointMock.register(builder)
  }

  def zipContentsOfDir(path: String): String = {
    val zipFilepath = FileUtils.getTempDirectory.toPath.resolve("test.zip").toString
    ZipUtil.pack(new File(path), new File(zipFilepath))
    zipFilepath
  }


}
