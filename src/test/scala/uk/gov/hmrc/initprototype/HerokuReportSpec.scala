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

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import scala.io.Source

class HerokuReportSpec extends AnyFunSpec with Matchers with MockitoSugar with AwaitSupport with BeforeAndAfterEach {
  val mockManager: HerokuManager = mock[HerokuManager]
  val herokuConfiguration: HerokuConfiguration = new HerokuConfiguration {
    override val administratorEmails: List[String] = List("admin@example.com")
  }

  override def beforeEach(): Unit = {
    when(mockManager.getAppNames)
      .thenReturn(Future.successful(Seq("my-other-app", "my-test-app")))
    when(mockManager.getAppReleases("my-other-app", range = None))
      .thenReturn(Future.successful(
        (Seq(HerokuRelease("2019-12-01", "First release"), HerokuRelease("2019-12-02", "Second release")), None)))
    when(mockManager.getAppReleases("my-test-app", range = None))
      .thenReturn(Future.successful(
        (Seq(
          HerokuRelease("2019-12-03", "First release"),
          HerokuRelease("2019-12-04", "Second release")
        ), None)))
    when(mockManager.getAppFormation("my-other-app"))
      .thenReturn(Future.successful(Some(HerokuFormation("Standard-1X", 1, HerokuApp("my-other-app")))))
    when(mockManager.getAppFormation("my-test-app"))
      .thenReturn(Future.successful(Some(HerokuFormation("Standard-2X", 2, HerokuApp("my-test-app")))))
  }

  describe("HerokuReportTask") {
    val herokuTask = new HerokuReportTask(mockManager, herokuConfiguration)

    describe("getAppsReleases") {


      it("should get apps releases") {
        val result: Seq[String] = await(herokuTask.getAppsReleases)

        result should equal(
          Seq(
            "name\tnumberOfUnits\tdynoSize\tnumberOfReleases\tcreated\tlastUpdated",
            "my-other-app\t1\tStandard-1X\t2\t2019-12-01\t2019-12-02",
            "my-test-app\t2\tStandard-2X\t2\t2019-12-03\t2019-12-04"
          ))
      }


      it("should exclude any releases with admin email addresses") {
        when(mockManager.getAppReleases("my-test-app", range = None))
          .thenReturn(Future.successful(
            (Seq(
              HerokuRelease("2019-12-03", "First release"),
              HerokuRelease("2019-12-04", "Second release"),
              HerokuRelease("2020-02-01", "Admin release", "admin@example.com")
            ), None)))

        val result: Seq[String] = await(herokuTask.getAppsReleases)

        result should equal(
          Seq(
            "name\tnumberOfUnits\tdynoSize\tnumberOfReleases\tcreated\tlastUpdated",
            "my-other-app\t1\tStandard-1X\t2\t2019-12-01\t2019-12-02",
            "my-test-app\t2\tStandard-2X\t2\t2019-12-03\t2019-12-04"
          ))
      }


      def createReportFile: String = {
        import java.io.File
        val file = File.createTempFile("heroku-report-test", "txt")
        file.deleteOnExit()
        file.getAbsolutePath
      }

      it("should write to a file") {
        val reportFile = createReportFile

        await(herokuTask.getAppsReleases(Array(reportFile)))

        val report = Source.fromFile(reportFile, "UTF-8")
        try {
          report.getLines.toSeq should equal(
            Seq(
              "name\tnumberOfUnits\tdynoSize\tnumberOfReleases\tcreated\tlastUpdated",
              "my-other-app\t1\tStandard-1X\t2\t2019-12-01\t2019-12-02",
              "my-test-app\t2\tStandard-2X\t2\t2019-12-03\t2019-12-04"
            ))
        } finally {
          report.close()
        }
      }

      it("should throw an error if no arguments are supplied") {
        val thrown = intercept[Exception] {
          await(herokuTask.getAppsReleases(Seq.empty))
        }

        thrown.getMessage should startWith regex "Missing path to apps file"
      }
    }
  }
}
