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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.io.{ByteArrayOutputStream}
import java.time.{Instant, Period}

class HerokuGenerateSpinDownListTaskSpec extends AnyFunSpec with BeforeAndAfterEach {
  val wireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  override def beforeEach {
    wireMockServer.start()
    WireMock.configureFor("localhost", wireMockServer.port())
  }

  override def afterEach {
    wireMockServer.stop()
  }

  describe("HerokuGenerateSpinDownListTask") {
    it("outputs link to the generated spin down list to the console") {
      val outputFile = os.temp()

      stubFor(
        get(urlEqualTo("/teams/digital-hmrc-gov/apps"))
          .withHeader("Authorization", matching("Bearer .+"))
          .withHeader("Accept", equalTo("application/vnd.heroku+json; version=3"))
          .willReturn(okJson("[]"))
      )

      val stdout = new ByteArrayOutputStream()
      Console.withOut(stdout) {
        generateSpinDownList(outputFile)
      }

      stdout.toString shouldEqual s"""
        |Spin down list: $outputFile
        |""".trim.stripMargin
    }

    it("outputs the names of apps to spin down to the provided output path on separate lines") {
      val outputFile = os.temp()

      givenAllAppsAreCurrentlyRunning

      stubFor(
        get(urlEqualTo("/teams/digital-hmrc-gov/apps"))
          .withHeader("Authorization", matching("Bearer .+"))
          .withHeader("Accept", equalTo("application/vnd.heroku+json; version=3"))
          .willReturn(okJson(s"""[
            {\"name\": \"app-1\", \"updated_at\": "${Instant.now.minus(Period.ofDays(100))}"},
            {\"name\": \"app-2\", \"updated_at\": "${Instant.now.minus(Period.ofDays(100))}"}
          ]"""))
      )

      generateSpinDownList(outputFile)

      os.read.lines(outputFile) shouldEqual Array(
        "app-1",
        "app-2"
      )
    }

    it("only proposes spinning down applications that are currently turned on") {
      val outputFile = os.temp()

      givenOnlySomeAppsAreCurrentlyRunning("app-1")

      stubFor(
        get(urlEqualTo("/teams/digital-hmrc-gov/apps"))
          .withHeader("Authorization", matching("Bearer .+"))
          .withHeader("Accept", equalTo("application/vnd.heroku+json; version=3"))
          .willReturn(okJson(s"""[
            {\"name\": \"app-1\", \"updated_at\": "${Instant.now.minus(Period.ofDays(100))}"},
            {\"name\": \"app-2\", \"updated_at\": "${Instant.now.minus(Period.ofDays(100))}"}
          ]"""))
      )

      generateSpinDownList(outputFile)

      os.read.lines(outputFile) shouldEqual Array(
        "app-1"
      )
    }

    it("only proposes spinning down apps not updated in the last 84 days") {
      val outputFile = os.temp()

      givenAllAppsAreCurrentlyRunning

      stubFor(
        get(urlEqualTo("/teams/digital-hmrc-gov/apps"))
          .withHeader("Authorization", matching("Bearer .+"))
          .withHeader("Accept", equalTo("application/vnd.heroku+json; version=3"))
          .willReturn(okJson(s"""[
            {\"name\": \"app-1\", \"updated_at\": "${Instant.now.minus(Period.ofDays(83))}"},
            {\"name\": \"app-2\", \"updated_at\": "${Instant.now.minus(Period.ofDays(100))}"},
            {\"name\": \"app-3\", \"updated_at\": "${Instant.now.minus(Period.ofDays(84))}"}
          ]"""))
      )

      generateSpinDownList(outputFile)

      os.read.lines(outputFile) shouldEqual Array(
        "app-2",
        "app-3"
      )
    }

    it("does not propose spinning down apps that have been requested to be kept running") {
      val outputFile = os.temp()

      givenAllAppsAreCurrentlyRunning

      stubFor(
        get(urlEqualTo("/teams/digital-hmrc-gov/apps"))
          .withHeader("Authorization", matching("Bearer .+"))
          .withHeader("Accept", equalTo("application/vnd.heroku+json; version=3"))
          .willReturn(okJson(s"""[
            {\"name\": \"app-1\", \"updated_at\": "${Instant.now.minus(Period.ofDays(83))}"},
            {\"name\": \"app-2\", \"updated_at\": "${Instant.now.minus(Period.ofDays(100))}"},
            {\"name\": \"app-3\", \"updated_at\": "${Instant.now.minus(Period.ofDays(84))}"}
          ]"""))
      )

      generateSpinDownList(outputFile, keepRunning = Set("app-3"))

      os.read.lines(outputFile) shouldEqual Array(
        "app-2"
      )
    }

    it(
      "can update an existing spin down list to remove apps that were updated or requested to be kept running without adding newly inactive apps"
    ) {
      val outputFile = os.temp()

      givenAllAppsAreCurrentlyRunning

      stubFor(
        get(urlEqualTo("/teams/digital-hmrc-gov/apps"))
          .withHeader("Authorization", matching("Bearer .+"))
          .withHeader("Accept", equalTo("application/vnd.heroku+json; version=3"))
          .willReturn(okJson(s"""[
            {\"name\": \"app-1\", \"updated_at\": "${Instant.now.minus(Period.ofDays(83))}"},
            {\"name\": \"app-2\", \"updated_at\": "${Instant.now.minus(Period.ofDays(100))}"},
            {\"name\": \"app-3\", \"updated_at\": "${Instant.now.minus(Period.ofDays(84))}"},
            {\"name\": \"app-4\", \"updated_at\": "${Instant.now.minus(Period.ofDays(100))}"}
          ]"""))
      )

      val stdoutFromFirstRun = new ByteArrayOutputStream()
      Console.withOut(stdoutFromFirstRun) {
        generateSpinDownList(outputFile)
      }
      os.read.lines(outputFile) shouldEqual Array(
        "app-2",
        "app-3",
        "app-4"
      )
      stdoutFromFirstRun.toString shouldEqual s"""
        |Spin down list: $outputFile
        |""".trim.stripMargin

      stubFor(
        get(urlEqualTo("/teams/digital-hmrc-gov/apps"))
          .withHeader("Authorization", matching("Bearer .+"))
          .withHeader("Accept", equalTo("application/vnd.heroku+json; version=3"))
          .willReturn(okJson(s"""[
            {\"name\": \"app-1\", \"updated_at\": "${Instant.now.minus(Period.ofDays(84))}"},
            {\"name\": \"app-2\", \"updated_at\": "${Instant.now.minus(Period.ofDays(1))}"},
            {\"name\": \"app-3\", \"updated_at\": "${Instant.now.minus(Period.ofDays(85))}"},
            {\"name\": \"app-4\", \"updated_at\": "${Instant.now.minus(Period.ofDays(101))}"}
          ]"""))
      )

      val stdoutFromSecondRun = new ByteArrayOutputStream()
      Console.withOut(stdoutFromSecondRun) {
        generateSpinDownList(outputFile, keepRunning = Set("app-4"))
      }
      os.read.lines(outputFile) shouldEqual Array(
        "app-3"
      )
      stdoutFromSecondRun.toString shouldEqual s"""
        |IMPORTANT: EXISTING SPIN DOWN LIST FOUND
        |===============================================
        |
        |When an existing spin down list is found, this
        |task will update it to remove apps updated in
        |the last week or were added to the list of apps
        |to keep running. No apps that became inactive
        |this week will be added, because they would not
        |have received notice of the plan to spin down
        |their app.
        |
        |If you instead meant to plan a new spin down,
        |then remove the file and rerun this task.
        |
        |Spin down list: $outputFile
        |""".stripMargin
    }

  }

  private def givenAllAppsAreCurrentlyRunning = stubFor(
    get(urlMatching("/apps/(.*)/formation"))
      .willReturn(okJson("""[{"quantity": 1}]""".stripMargin))
  )

  private def givenOnlySomeAppsAreCurrentlyRunning(apps: String*): Unit = {
    stubFor(
      get(urlMatching("/apps/(.*)/formation"))
        .withHeader("Authorization", matching("Bearer .+"))
        .withHeader("Accept", equalTo("application/vnd.heroku+json; version=3"))
        .willReturn(okJson("""[{"quantity": 0}]""".stripMargin))
    )
    apps.foreach(app =>
      stubFor(
        get(urlEqualTo(s"/apps/$app/formation"))
          .withHeader("Authorization", matching("Bearer .+"))
          .withHeader("Accept", equalTo("application/vnd.heroku+json; version=3"))
          .willReturn(okJson("""[{"quantity": 1}]""".stripMargin))
      )
    )
  }

  private def generateSpinDownList(outputPath: os.Path, keepRunning: Set[String] = Set.empty): Unit =
    new HerokuGenerateSpinDownListTask(new HerokuConfiguration {
      override val baseUrl                              = s"http://localhost:${wireMockServer.port()}"
      override val herokuAppsToKeepRunning: Set[String] = keepRunning
    }).generateSpinDownList(outputPath)
}
