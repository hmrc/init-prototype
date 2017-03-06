/*
 * Copyright 2017 HM Revenue & Customs
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

import ammonite.ops.{%, _}
import ch.qos.logback.classic.{Level, Logger}
import org.slf4j
import org.slf4j.LoggerFactory
import uk.gov.hmrc.initprototype.ArgParser.Config

object Main {


  val logger = com.typesafe.scalalogging.Logger("init-prototype")


  def main(args: Array[String]) {

    val root: Logger = LoggerFactory.getLogger(slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    ArgParser.parser.parse(args, Config()) foreach { config =>
      root.setLevel(Level.INFO)
      if (config.verbose) {
        root.setLevel(Level.DEBUG)
      } else {
        root.setLevel(Level.INFO)
      }

      start(config)
    }
  }

  private def getZipBallArtifactUrl(payloadDetails: Config): String = {
    import payloadDetails._
    s"$gitApiBaseUrl/repos/$org/$templateRepoName/zipball/master"
  }


  def gitInit(localRepoPath: String, repoName: String, token: String) = {
    logger.debug(s"$localRepoPath")
    val dir = Path(localRepoPath)
    %('git, "init" , ".")(dir)
    %('git, "add", ".", "-A")(dir)
    %('git, "commit" , "-m", s"Creating new prototype $repoName")(dir)
    %('git, "remote" , "add", "origin", s"https://$token:x-oauth-basic@github.tools.tax.service.gov.uk/HMRC/$repoName.git")(dir)
    %('git, "push" , "--set-upstream", "origin", "master")(dir)
  }


  def start(config: Config): Unit = {
    val credentials = GithubCredentials(config.credentialsFile)

    val githubZipUri = getZipBallArtifactUrl(config)
    val localRepoPath = GithubArtifactDownloader.getRepoZipAndExplode(githubZipUri, credentials)
    gitInit(localRepoPath, config.targetRepoName, credentials.token)
    

  }

}
