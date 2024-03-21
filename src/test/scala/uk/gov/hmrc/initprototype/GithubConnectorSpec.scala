/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class GithubConnectorSpec extends AnyFunSpec with Matchers with WireMockEndpoints {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val config: GithubConfiguration = new GithubConfiguration {
    override val baseUrl: String = endpointMockUrl
  }

  describe("GithubConnector") {
    val githubConnector = new GithubConnector(config)

    describe("getGithubRepoName") {
      it("should return the repo name related to a given github commit SHA") {
        val maybeGithubRepo = githubConnector.getGithubRepoName("some-commit-sha")
        maybeGithubRepo should be(Some("estates-registration-iv-prototype"))
      }

      it("should return None if the repo name isn't found") {
        val maybeGithubRepo = githubConnector.getGithubRepoName("some-other-commit-sha")
        maybeGithubRepo should be(None)
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: GithubConfiguration = new GithubConfiguration {
          override val apiToken        = "incorrect-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherGithubConnector                 = new GithubConnector(incorrectConfig)

        val thrown = intercept[Exception] {
          otherGithubConnector.getGithubRepoName("some-commit-sha")
        }

        thrown.getMessage should startWith regex "Error with Github API request"
      }
    }

    describe("repoExists") {
      it("should return true if the repo exists in Github") {
        val repoExists = githubConnector.repoExists(repo = "some-repo", owner = "some-owner")
        repoExists should be(true)
      }

      it("should return false if the repo doesn't exist") {
        val repoExists = githubConnector.repoExists(repo = "some-other-repo", owner = "some-owner")
        repoExists should be(false)
      }

      it("should default repo owner to hmrc") {
        val repoExists = githubConnector.repoExists(repo = "some-other-repo")
        repoExists should be(true)
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: GithubConfiguration = new GithubConfiguration {
          override val apiToken        = "incorrect-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherGithubConnector                 = new GithubConnector(incorrectConfig)

        val thrown = intercept[Exception] {
          otherGithubConnector.repoExists(repo = "some-other-repo")
        }

        thrown.getMessage should startWith regex "Error with Github API request"
      }
    }

    describe("fileExists") {
      it("should return true if the file exists in the given Github repo") {
        val fileExists = githubConnector.fileExists(repo = "some-repo", owner = "some-owner", path = "some/file")
        fileExists should be(true)
      }

      it("should return false if the file doesn't exist") {
        val fileExists = githubConnector.fileExists(repo = "some-other-repo", owner = "some-owner", path = "some/file")
        fileExists should be(false)
      }

      it("should default repo owner to hmrc") {
        val fileExists = githubConnector.fileExists(repo = "some-other-repo", path = "some/file")
        fileExists should be(true)
      }

      it("should throw an error if the server responds with a 401") {
        val incorrectConfig: GithubConfiguration = new GithubConfiguration {
          override val apiToken        = "incorrect-token"
          override val baseUrl: String = endpointMockUrl
        }
        val otherGithubConnector                 = new GithubConnector(incorrectConfig)

        val thrown = intercept[Exception] {
          otherGithubConnector.fileExists(repo = "some-other-repo", path = "some/file")
        }

        thrown.getMessage should startWith regex "Error with Github API request"
      }
    }
  }
}
