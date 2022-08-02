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

import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, patchRequestedFor, urlEqualTo}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import com.fasterxml.jackson.core.JsonParseException
import scala.concurrent.duration._
import scala.language.postfixOps

class HerokuManagerSpec
    extends AnyFunSpec
    with BeforeAndAfterEach
    with Matchers
    with WireMockEndpoints
    with AwaitSupport {
  val config: HerokuConfiguration = new HerokuConfiguration {
    override val baseUrl: String = endpointMockUrl
  }

  describe("HerokuManager") {
    val herokuManager = new HerokuManager(config)

    describe("getAppNames") {
      it("should get all the apps, paginating if required") {
        val apps: Seq[String] = await(herokuManager.getAppNames)

        apps.size should be(3)
        apps.head should equal("my-app")
        apps(1)   should equal("my-other-app")
        apps(2)   should equal("yet-another-app")
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken        = "incorrect-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherHerokuManager                   = new HerokuManager(incorrectConfig)

        val thrown = intercept[Exception] {
          await(otherHerokuManager.getAppNames)
        }

        thrown.getMessage should startWith regex "Error with Heroku request"
      }

      it("should throw an error if the response is malformed JSON") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken        = "malformed-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherHerokuManager                   = new HerokuManager(incorrectConfig)

        val thrown = intercept[JsonParseException] {
          await(otherHerokuManager.getAppNames)
        }

        thrown.getMessage should startWith regex "Unrecognized token"
      }

      it("should call the Heroku api") {
        await(herokuManager.getAppNames)

        endpointServer.verify(getRequestedFor(urlEqualTo("/apps/")))
      }
    }

    describe("getAppReleasesFromRange") {
      it("should get the releases for the given app") {
        val (releases: Seq[HerokuRelease], _) = await(herokuManager.getAppReleasesFromRange("any-app", None))

        releases.size             should be > 0
        releases.head.description should equal("Initial release")
      }

      it("should get the next page of releases for the given app") {
        val (releases: Seq[HerokuRelease], _) =
          await(herokuManager.getAppReleasesFromRange("any-app", Some("version ]200..")))

        releases.size             should be > 0
        releases.head.description should equal("Third release")
      }

      it("should return the correct next range token") {
        val (releases: Seq[HerokuRelease], nextRange) = await(herokuManager.getAppReleasesFromRange("any-app", None))

        releases.size should be > 0
        nextRange     should equal(Some("version ]200.."))
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken        = "incorrect-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherHerokuManager                   = new HerokuManager(incorrectConfig)

        val thrown = intercept[Exception] {
          await(otherHerokuManager.getAppReleasesFromRange("my-sample-app", None))
        }

        thrown.getMessage should startWith regex "Error with Heroku request"
      }

      it("should throw an error if the response is malformed JSON") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken        = "malformed-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherHerokuManager                   = new HerokuManager(incorrectConfig)

        val thrown = intercept[JsonParseException] {
          await(otherHerokuManager.getAppReleasesFromRange("my-sample-app", None))
        }

        thrown.getMessage should startWith regex "Unrecognized token"
      }

      it("should call the Heroku api") {
        await(herokuManager.getAppReleasesFromRange("my-other-app", None))

        endpointServer.verify(getRequestedFor(urlEqualTo("/apps/my-other-app/releases/")))
      }
    }

    describe("getAppFormation") {
      it("should get the formation for the given app") {
        val formation: Option[HerokuFormation] = await(herokuManager.getAppFormation("any-app"))

        formation.isDefined should be(true)
        formation.get.size  should equal("Standard-1X")
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken        = "incorrect-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherHerokuManager                   = new HerokuManager(incorrectConfig)

        val thrown = intercept[Exception] {
          await(otherHerokuManager.getAppFormation("my-sample-app"))
        }

        thrown.getMessage should startWith regex "Error with Heroku request"
      }

      it("should throw an error if the response is malformed JSON") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken        = "malformed-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherHerokuManager                   = new HerokuManager(incorrectConfig)

        val thrown = intercept[JsonParseException] {
          await(otherHerokuManager.getAppFormation("my-sample-app"))
        }

        thrown.getMessage should startWith regex "Unrecognized token"
      }

      it("should call the Heroku api") {
        await(herokuManager.getAppFormation("my-other-app"))

        endpointServer.verify(getRequestedFor(urlEqualTo("/apps/my-other-app/formation/")))
      }
    }

    describe("getAppReleases") {
      it("should get all the releases for the given app") {
        val (releases, _) = await(herokuManager.getAppReleases("any-app", range = None), 10 second)

        releases.size             should be(5)
        releases.head.description should equal("Initial release")
        releases(4).description   should equal("Enable allow-multiple-sni-endpoints feature")
      }
    }

    describe("spinDown") {
      it("should set the dyno count of the given app to zero") {
        val app: HerokuFormation = await(herokuManager.spinDownApp("my-sample-app"))

        app.quantity should equal(0)
        app.app.name should equal("my-test-app")
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken        = "incorrect-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherHerokuManager                   = new HerokuManager(incorrectConfig)

        intercept[Exception] {
          await(otherHerokuManager.spinDownApp("my-sample-app"))
        }
      }

      it("should throw an error if the response is malformed JSON") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken        = "malformed-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherHerokuManager                   = new HerokuManager(incorrectConfig)

        val thrown = intercept[JsonParseException] {
          await(otherHerokuManager.spinDownApp("my-sample-app"))
        }

        thrown.getMessage should startWith regex "Unrecognized token"
      }

      it("should call the Heroku api") {
        await(herokuManager.spinDownApp("my-other-app"))

        endpointServer.verify(patchRequestedFor(urlEqualTo("/apps/my-other-app/formation/web")))
      }
    }

    describe("getSlugIds") {
      it("should get the first 10 slug ids associated with releases for the given app, in ascending version order") {
        val slugIds = await(herokuManager.getSlugIds("app-with-11-release-slugs"), 10 second)

        slugIds.size should be(10)
        slugIds.head should equal("First slug")
        slugIds(9)   should equal("Tenth slug")
      }
    }

    describe("getCommitId") {
      it("should get the commit id associated with the given application slug") {
        val maybeSlugId = await(herokuManager.getCommitId("some-app", "some-slug"), 10 second)
        maybeSlugId should be(Some("some-commit-id"))
      }
    }

  }
}
