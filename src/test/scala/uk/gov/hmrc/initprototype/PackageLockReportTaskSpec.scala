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

import org.mockito.ArgumentMatchers.{any, eq => is}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.io.PrintWriter
import scala.concurrent.Future
import scala.io.Source

class PackageLockReportTaskSpec
    extends AnyFunSpec
    with Matchers
    with MockitoSugar
    with AwaitSupport
    with BeforeAndAfterEach {

  val mockGithubConnector: GithubConnector = mock[GithubConnector]
  val mockHerokuConnector: HerokuConnector = mock[HerokuConnector]

  when(mockGithubConnector.fileExists(any[String], any[String], any[String])).thenReturn(true)

  // first prototype does not exist in github
  when(mockGithubConnector.repoExists(is("first-prototype"), any[String])).thenReturn(false)
  when(mockHerokuConnector.getSlugIds(is("first-prototype"))).thenReturn(Future(Nil))
  when(mockHerokuConnector.getSlugIds(is("first-prototype"))).thenReturn(Future(Seq("slug1")))
  when(mockHerokuConnector.getCommitId(is("first-prototype"), is("slug1"))).thenReturn(Future(None))

  // second prototype exists under a different repo name, but has no package.json or package-lock.json
  when(mockGithubConnector.repoExists(is("second-prototype"), any[String])).thenReturn(false)
  when(mockHerokuConnector.getSlugIds(is("second-prototype"))).thenReturn(Future(Seq("slug1", "slug2", "slug3")))
  when(mockHerokuConnector.getCommitId(is("second-prototype"), is("slug1"))).thenReturn(Future(None))
  when(mockHerokuConnector.getCommitId(is("second-prototype"), is("slug2"))).thenReturn(Future(Some("sha2")))
  when(mockGithubConnector.getGithubRepoName(is("sha2"))).thenReturn(None)
  when(mockHerokuConnector.getCommitId(is("second-prototype"), is("slug3"))).thenReturn(Future(Some("sha3")))
  when(mockGithubConnector.getGithubRepoName(is("sha3"))).thenReturn(Some("second-repo-name"))
  when(mockGithubConnector.fileExists(any[String], is("second-repo-name"), any[String])).thenReturn(false)

  // third prototype is missing a package-lock.json
  when(mockGithubConnector.repoExists(is("third-prototype"), any[String])).thenReturn(true)
  when(mockGithubConnector.fileExists(is("package-lock.json"), is("third-prototype"), any[String])).thenReturn(false)

  // fourth prototype has a package-lock.json
  when(mockGithubConnector.repoExists(is("fourth-prototype"), any[String])).thenReturn(true)

  val config = new PackageLockReportConfiguration {
    override val packageLockReportFile: String = "test-package-lock-report.txt"
    override val herokuUsageReportFile: String = "test-heroku-report.txt"
  }

  override def beforeEach(): Unit =
    setupHerokuUsageReport()

  describe("PackageLockReportTask") {

    val reportTask = new PackageLockReportTask(mockHerokuConnector, mockGithubConnector, config)

    describe("generateReport") {

      it("should read the Heroku usage report and write an enriched report to a file") {
        await(reportTask.generateReport())

        val testSource  = Source.fromFile((os.pwd / config.packageLockReportFile).toString())
        val actualLines = testSource.getLines().toList

        val expectedLines = List(
          "prototypeName\tgithubRepoName\tcontainsPackageJson\tcontainsPackageLockJson\tdeployedToHeroku\trunningInHeroku\tinGitHub",
          "first-prototype\tunknown\tunknown\tunknown\ttrue\ttrue\tunknown",
          "second-prototype\tsecond-repo-name\tfalse\tfalse\ttrue\ttrue\ttrue",
          "third-prototype\tthird-prototype\ttrue\tfalse\ttrue\ttrue\ttrue",
          "fourth-prototype\tfourth-prototype\ttrue\ttrue\ttrue\ttrue\ttrue"
        )
        actualLines shouldEqual expectedLines
      }

    }
  }

  private def setupHerokuUsageReport(): Unit = {
    val printWriter = new PrintWriter((os.pwd / config.herokuUsageReportFile).toString())
    val rows        = List(
      s"name\tnumberOfUnits\tdynoSize\tnumberOfReleases\tcreated\tlastUpdated",
      s"first-prototype\t0\tStandard\t6\t2019-08-16T08:08:08Z\t2021-07-20T15:32:04Z",
      s"second-prototype\t1\tStandard\t6\t2019-08-16T08:08:08Z\t2021-07-20T15:32:04Z",
      s"third-prototype\t1\tStandard\t6\t2019-08-16T08:08:08Z\t2021-07-20T15:32:04Z",
      s"fourth-prototype\t1\tStandard\t6\t2019-08-16T08:08:08Z\t2021-07-20T15:32:04Z"
    )
    printWriter.print(rows.mkString("\n"))
    printWriter.close()
  }

}
