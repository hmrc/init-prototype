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

import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, patchRequestedFor, urlEqualTo}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class HerokuManagerSpec
    extends AnyFunSpec
    with BeforeAndAfterEach
    with Matchers
    with WireMockEndpoints
    with AwaitSupport {
  implicit val config: HerokuConfiguration = new HerokuConfiguration {
    override val baseUrl: String = endpointMockUrl
  }

  describe("HerokuManager") {
    val herokuManager = new HerokuManager

    describe("getApps") {
      it("should get all the apps") {
        val apps: Seq[HerokuApp] = await(herokuManager.getApps)

        apps.size      should be > 1
        apps.head.name should equal("my-app")
        apps(1).name   should equal("my-other-app")
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken        = "incorrect-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherHerokuManager = new HerokuManager()(incorrectConfig, implicitly[ExecutionContext])

        val thrown = intercept[Exception] {
          await(otherHerokuManager.getApps)
        }

        thrown.getMessage should startWith regex "Error with Heroku request"
      }

      it("should call the Heroku api") {
        await(herokuManager.getApps)

        endpointServer.verify(getRequestedFor(urlEqualTo("/apps/")))
      }
    }

    describe("getAppNames") {
      it("should get all the apps") {
        val apps = await(herokuManager.getAppNames)

        apps.size should be(2)
        apps.head should equal("my-app")
        apps(1)   should equal("my-other-app")
      }
    }

    describe("getAppReleases") {
      it("should get the releases for the given app") {
        val (releases: Seq[HerokuRelease], _) = await(herokuManager.getAppReleases("any-app"))

        releases.size             should be > 0
        releases.head.description should equal("Initial release")
      }

      it("should get the next page of releases for the given app") {
        val (releases: Seq[HerokuRelease], _) =
          await(herokuManager.getAppReleases("any-app", Some("version ]200..")))

        releases.size             should be > 0
        releases.head.description should equal("Third release")
      }

      it("should return the correct next range token") {
        val (releases: Seq[HerokuRelease], nextRange) = await(herokuManager.getAppReleases("any-app"))

        releases.size should be > 0
        nextRange     should equal(Some("version ]200.."))
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken        = "incorrect-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherHerokuManager = new HerokuManager()(incorrectConfig, implicitly[ExecutionContext])

        val thrown = intercept[Exception] {
          await(otherHerokuManager.getAppReleases("my-sample-app"))
        }

        thrown.getMessage should startWith regex "Error with Heroku request"
      }

      it("should call the Heroku api") {
        await(herokuManager.getAppReleases("my-other-app"))

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
        val otherHerokuManager = new HerokuManager()(incorrectConfig, implicitly[ExecutionContext])

        val thrown = intercept[Exception] {
          await(otherHerokuManager.getAppFormation("my-sample-app"))
        }

        thrown.getMessage should startWith regex "Error with Heroku request"
      }

      it("should call the Heroku api") {
        await(herokuManager.getAppFormation("my-other-app"))

        endpointServer.verify(getRequestedFor(urlEqualTo("/apps/my-other-app/formation/")))
      }
    }

    describe("getAppReleasesRecursive") {
      it("should get all the releases for the given app") {
        val (releases, _) = await(herokuManager.getAppReleasesRecursive("any-app"), 10 second)

        releases.size             should be(4)
        releases.head.description should equal("Initial release")
        releases(3).description   should equal("Fourth release")
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
        val otherHerokuManager = new HerokuManager()(incorrectConfig, implicitly[ExecutionContext])

        intercept[Exception] {
          await(otherHerokuManager.spinDownApp("my-sample-app"))
        }
      }

      it("should call the Heroku api") {
        await(herokuManager.spinDownApp("my-other-app"))

        endpointServer.verify(patchRequestedFor(urlEqualTo("/apps/my-other-app/formation/web")))
      }
    }
  }
}
