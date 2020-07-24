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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, patchRequestedFor, urlEqualTo}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsObject, JsValue}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class HerokuManagerSpec extends AnyFunSpec with BeforeAndAfterEach with Matchers {
  private val port           = 8080
  private val wireMockServer = new WireMockServer(options().port(port))

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val config: HerokuConfiguration = new HerokuConfiguration {
    override val apiToken = "dummy-token"
    override val baseUrl  = s"http://localhost:$port"
  }

  describe("HerokuManager") {
    val herokuManager = new HerokuManager

    describe("getApps") {
      it("should get all the apps") {
        val apps = Await.result(herokuManager.getApps, 1 second)

        (apps.size)                   should be > 1
        (apps(0) \ "name").as[String] should equal("my-app")
        (apps(1) \ "name").as[String] should equal("my-other-app")
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken = "incorrect-token"
          override val baseUrl  = s"http://localhost:$port"
        }
        val otherHerokuManager = new HerokuManager()(incorrectConfig, implicitly[ExecutionContext])

        val thrown = intercept[Exception] {
          Await.result(otherHerokuManager.getApps, 1000 millis)
        }

        thrown.getMessage should startWith regex "Error with Heroku request"
      }

      it("should call the Heroku api") {
        Await.result(herokuManager.getApps, 1000 millis)

        wireMockServer.verify(getRequestedFor(urlEqualTo("/apps/")))
      }
    }

    describe("getAppNames") {
      it("should get all the apps") {
        val apps = Await.result(herokuManager.getAppNames, 1 second)

        (apps.size) should be(2)
        apps(0)     should equal("my-app")
        apps(1)     should equal("my-other-app")
      }
    }

    describe("getAppReleases") {
      it("should get the releases for the given app") {
        val (releases: Seq[JsObject], _) = Await.result(herokuManager.getAppReleases("any-app"), 1 second)

        (releases.size)                          should be > 0
        (releases(0) \ "description").as[String] should equal("Initial release")
      }

      it("should get the next page of releases for the given app") {
        val (releases: Seq[JsObject], _) =
          Await.result(herokuManager.getAppReleases("any-app", Some("version ]200..")), 1 second)

        (releases.size)                          should be > 0
        (releases(0) \ "description").as[String] should equal("Third release")
      }

      it("should return the correct next range token") {
        val (releases: Seq[JsObject], nextRange) = Await.result(herokuManager.getAppReleases("any-app"), 1 second)

        (releases.size) should be > 0
        nextRange       should equal(Some("version ]200.."))
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken = "incorrect-token"
          override val baseUrl  = s"http://localhost:$port"
        }
        val otherHerokuManager = new HerokuManager()(incorrectConfig, implicitly[ExecutionContext])

        val thrown = intercept[Exception] {
          Await.result(otherHerokuManager.getAppReleases("my-sample-app"), 1000 millis)
        }

        thrown.getMessage should startWith regex "Error with Heroku request"
      }

      it("should call the Heroku api") {
        Await.result(herokuManager.getAppReleases("my-other-app"), 1000 millis)

        wireMockServer.verify(getRequestedFor(urlEqualTo("/apps/my-other-app/releases/")))
      }
    }

    describe("getAppFormation") {
      it("should get the formation for the given app") {
        val formation: Option[JsObject] = Await.result(herokuManager.getAppFormation("any-app"), 1 second)

        (formation.isDefined)               should be(true)
        (formation.get \ "size").as[String] should equal("Standard-1X")
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken = "incorrect-token"
          override val baseUrl  = s"http://localhost:$port"
        }
        val otherHerokuManager = new HerokuManager()(incorrectConfig, implicitly[ExecutionContext])

        val thrown = intercept[Exception] {
          Await.result(otherHerokuManager.getAppFormation("my-sample-app"), 1000 millis)
        }

        thrown.getMessage should startWith regex "Error with Heroku request"
      }

      it("should call the Heroku api") {
        Await.result(herokuManager.getAppFormation("my-other-app"), 1000 millis)

        wireMockServer.verify(getRequestedFor(urlEqualTo("/apps/my-other-app/formation/")))
      }
    }

    describe("getAppQuantityAndSize") {
      it("should get the quantity and size for the given app") {
        val tupleOption = Await.result(herokuManager.getAppQuantityAndSize("any-app"), 1 second)

        tupleOption.isDefined should be(true)
        val (quantity, size) = tupleOption.get
        size     should be("Standard-1X")
        quantity should be(1)
      }
    }

    describe("getAppReleasesRecursive") {
      it("should get all the releases for the given app") {
        val (releases, _) = Await.result(herokuManager.getAppReleasesRecursive("any-app"), 10 second)

        (releases.size)                          should be(4)
        (releases(0) \ "description").as[String] should equal("Initial release")
        (releases(3) \ "description").as[String] should equal("Fourth release")
      }
    }

    describe("spinDown") {
      it("should set the dyno count of the given app to zero") {
        val app: JsValue = Await.result(herokuManager.spinDownApp("my-sample-app"), 1000 millis)

        (app \ "quantity").as[Int]        should equal(0)
        (app \ "app" \ "name").as[String] should equal("my-test-app")
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: HerokuConfiguration = new HerokuConfiguration {
          override val apiToken = "incorrect-token"
          override val baseUrl  = s"http://localhost:$port"
        }
        val otherHerokuManager = new HerokuManager()(incorrectConfig, implicitly[ExecutionContext])

        intercept[Exception] {
          Await.result(otherHerokuManager.spinDownApp("my-sample-app"), 1000 millis)
        }
      }

      it("should call the Heroku api") {
        Await.result(herokuManager.spinDownApp("my-other-app"), 1000 millis)

        wireMockServer.verify(patchRequestedFor(urlEqualTo("/apps/my-other-app/formation/web")))
      }
    }
  }

  override def beforeEach {
    wireMockServer.start()
  }

  override def afterEach {
    wireMockServer.stop()
  }
}
