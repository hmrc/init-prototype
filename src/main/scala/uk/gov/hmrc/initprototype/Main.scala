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
import org.apache.commons.io.FileUtils
import org.slf4j
import org.slf4j.LoggerFactory
import uk.gov.hmrc.initprototype.ArgParser.Config

import scala.util.{Failure, Success, Try}

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



  def gitInit(localRepoPath: String, config: Config, token: String) = {

    def getGithubApiUri(pta: String) = s"https://$pta:x-oauth-basic@${config.targetGithubHost}/${config.targetOrg}/${config.targetRepoName}.git"

    logger.debug(s"$localRepoPath")
    val dir = Path(localRepoPath)
    %('git, "init" , ".")(dir)
    %('git, "add", ".", "-A")(dir)
    %('git, "commit" , "-m", s"Creating new prototype ${config.targetRepoName}")(dir)
    %('git, "remote", "add", "origin", getGithubApiUri(token))(dir)

    // we use Try to protect the token from being printed on the console in case of an error
    val tryOfPushResult = Try(%%('git, "push", "--set-upstream", "origin", "master")(dir))

    def throwError = throw new RuntimeException(s"Unable to push to remote repo ${getGithubApiUri("xxxxxxxx")}")

    tryOfPushResult match {
      case Success(pushResult) =>
        if (pushResult.exitCode != 0) {
          throwError
        }
      case Failure(t) => throwError
    }

  }


  def start(config: Config): Unit = {
    val credentials = GithubCredentials(config.credentialsFile)

    val eitherErrorOrUrl = PrototypeKitReleaseUrlResolver.getLatestZipballUrl(config.templateRepoApiUrl)

    eitherErrorOrUrl match {
      case Left(e) =>
        throw new RuntimeException(e)
      case Right(githubZipUrl) =>
        val artifactDownloadPath = FileUtils.getTempDirectory.toPath.resolve("prototype-template-archive.zip").toString
        val localRepoPath = new GithubArtifactDownloader(artifactDownloadPath).getRepoZipAndExplode(githubZipUrl)
        gitInit(localRepoPath, config, credentials.token)
    }
  }

}
