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

import os.{CommandResult, proc}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class GithubRepositoryDownloader {

  def checkoutRepository(repository: String, into: String): CommandResult =
    checkoutRepositories(List(repository), into).head

  def checkoutRepositories(repositories: List[String], into: String): Seq[CommandResult] = {
    val results: Seq[Future[CommandResult]] = repositories.map { repository =>
      Future {
        proc("git", "clone", "--depth", "1", s"https://github.com/hmrc/$repository.git")
          .call(cwd = os.pwd / into, check = false)
      }
    }
    Await.result(Future.sequence(results), Duration.Inf)
  }
}
