/*
 * Copyright 2022 HM Revenue & Customs
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

import org.apache.commons.io.FileUtils
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import scala.io.Source

class PackageLockReportTaskSpec
    extends AnyFunSpec
    with Matchers
    with MockitoSugar
    with AwaitSupport
    with BeforeAndAfterEach {

  val mockGithubDownloader = mock[GithubRepositoryDownloader]
  when(mockGithubDownloader.checkoutRepositories(any[List[String]](), anyString()))
    .thenReturn(Seq.empty)

  val config = new PackageLockReportConfiguration {
    override val prototypesDirectory: String   = "test-prototypes"
    override val packageLockReportFile: String = "test-package-lock-report.txt"
    override val herokuUsageReportFile: String = "test-heroku-report.txt"
  }

  override def beforeEach(): Unit =
    setupHerokuUsageReport()

  override def afterEach(): Unit = {
    FileUtils.deleteDirectory(new File(config.prototypesDirectory))
    FileUtils.forceDelete(new File(config.packageLockReportFile))
    FileUtils.forceDelete(new File(config.herokuUsageReportFile))
  }

  describe("PackageLockReportTask") {

    val reportTask = new PackageLockReportTask(mockGithubDownloader, config)

    describe("checkoutAndReport") {

      it("should checkout repositories from Heroku usage report") {
        val args: Array[String] = new Array[String](0)
        await(reportTask.checkoutAndReport(args))

        val expectedNames = List("first-repo", "second-repo", "third-repo")
        verify(mockGithubDownloader, times(1)).checkoutRepositories(expectedNames, config.prototypesDirectory)
      }

      it("should write report rows to a file from a Heroku usage report") {
        val args: Array[String] = new Array[String](0)
        await(reportTask.checkoutAndReport(args))

        val testSource  = Source.fromFile((os.pwd / config.packageLockReportFile).toString())
        val actualLines = testSource.getLines().toList

        val expectedLines = List(
          "prototypeName\tcontainsPackageJson\tcontainsPackageLockJson\tdeployedToHeroku\trunningInHeroku\tinGitHub",
          "first-repo\tunknown\tunknown\ttrue\ttrue\tfalse",
          "second-repo\tunknown\tunknown\ttrue\ttrue\tfalse",
          "third-repo\tunknown\tunknown\ttrue\ttrue\tfalse"
        )
        actualLines shouldEqual expectedLines
      }

      it("should update Heroku usage report rows based on Github repos") {
        val args: Array[String] = new Array[String](0)

        setupGithubDirectories()
        await(reportTask.checkoutAndReport(args))

        val testSource  = Source.fromFile((os.pwd / config.packageLockReportFile).toString())
        val actualLines = testSource.getLines().toList

        val expectedLines = List(
          "prototypeName\tcontainsPackageJson\tcontainsPackageLockJson\tdeployedToHeroku\trunningInHeroku\tinGitHub",
          "first-repo\tunknown\tunknown\ttrue\ttrue\tfalse",
          "second-repo\ttrue\tfalse\ttrue\ttrue\ttrue",
          "third-repo\ttrue\ttrue\ttrue\ttrue\ttrue"
        )

        actualLines shouldBe expectedLines
      }
    }
  }

  private def setupHerokuUsageReport() = {
    val herokuUsageReport = new File(config.herokuUsageReportFile)
    val printWriter       = new PrintWriter((os.pwd / config.herokuUsageReportFile).toString())
    val rows              = List(
      s"name\tnumberOfUnits\tdynoSize\tnumberOfReleases\tcreated\tlastUpdated",
      s"first-repo\t0\tStandard\t6\t2019-08-16T08:08:08Z\t2021-07-20T15:32:04Z",
      s"second-repo\t1\tStandard\t6\t2019-08-16T08:08:08Z\t2021-07-20T15:32:04Z",
      s"third-repo\t1\tStandard\t6\t2019-08-16T08:08:08Z\t2021-07-20T15:32:04Z"
    )
    printWriter.print(rows.mkString("\n"))
    printWriter.close()
  }

  private def setupGithubDirectories() = {
    val repoTwo                  = Paths.get(s"${config.prototypesDirectory}/second-repo")
    val repoThree                = Paths.get(s"${config.prototypesDirectory}/third-repo")
    val repoTwoPackageJson       = Paths.get(s"${config.prototypesDirectory}/second-repo/package.json")
    val repoThreePackageJson     = Paths.get(s"${config.prototypesDirectory}/third-repo/package.json")
    val repoThreePackageLockJson = Paths.get(s"${config.prototypesDirectory}/third-repo/package-lock.json")

    Files.createDirectories(repoTwo)
    Files.createDirectories(repoThree)
    Files.createFile(repoTwoPackageJson)
    Files.createFile(repoThreePackageJson)
    Files.createFile(repoThreePackageLockJson)
  }
}
