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

import java.io.File

import ammonite.ops.{%, _}
import ch.qos.logback.classic.{Level, Logger}
import org.apache.commons.io.FileUtils
import org.slf4j
import org.slf4j.LoggerFactory
import uk.gov.hmrc.initprototype.ArgParser.Config
import uk.gov.hmrc.initprototype.PrototypeKitReleaseUrlResolver.{ErrorMessage, ZipBallUrl}

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

  def gitClone(localRepoPath: String, config: Config, token: String) = {

    val repoUrl =
      s"https://$token:x-oauth-basic@${config.targetGithubHost}/${config.targetOrg}/${config.targetRepoName}.git"

    logger.debug(s"Cloning to: $localRepoPath")
    val dir = Path(localRepoPath)

    val repoPath = new File(localRepoPath).toPath.resolve(config.targetRepoName).toString
    %('rm, "-rf", repoPath)(dir)

    %('git, "clone", repoUrl)(dir)
  }

  def gitPush(localRepoPath: String, config: Config) = {

    val dir = Path(localRepoPath)
    %('git, "add", ".", "-A")(dir)
    %('git, "commit", "-m", s"Creating new prototype ${config.targetRepoName}")(dir)

    logger.debug(s"Pushing: $localRepoPath")
    // we use Try to protect the token from being printed on the console in case of an error
    val tryOfPushResult = Try(%%('git, "push")(dir))

    tryOfPushResult match {
      case Success(pushResult) =>
        if (pushResult.exitCode != 0) {
          throw new RuntimeException(s"Unable to push to remote repo.")
        }
      case Failure(t)          => throw new RuntimeException(s"Unable to push to remote repo.")
    }

  }

  def start(config: Config): Unit = {
    val credentials = GithubCredentials(config.githubUsername, config.githubToken)

    val eitherErrorOrUrl: Either[ErrorMessage, ZipBallUrl] =
      PrototypeKitReleaseUrlResolver.getLatestZipballUrl(config.templateRepoApiUrl, Some(config.githubToken))

    eitherErrorOrUrl match {
      case Left(e)             =>
        throw new RuntimeException(e)
      case Right(githubZipUrl) =>
        val tempPath             = FileUtils.getTempDirectory.toPath
        gitClone(tempPath.toString, config, credentials.token)
        val artifactDownloadPath = tempPath.resolve("prototype-template-archive.zip").toString
        val localExplodedPath    = new GithubArtifactDownloader().getRepoZipAndExplode(githubZipUrl, artifactDownloadPath)
        val localRepoPath        = new File(tempPath.toString).toPath.resolve(config.targetRepoName)
        FileUtils.copyDirectory(new File(localExplodedPath), localRepoPath.toFile)
        gitPush(localRepoPath.toString, config)
    }
  }

}
