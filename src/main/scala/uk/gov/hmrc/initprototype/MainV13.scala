/*
 * Copyright 2023 HM Revenue & Customs
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
import org.apache.commons.io.filefilter.FileFilterUtils
import org.slf4j
import org.slf4j.LoggerFactory
import uk.gov.hmrc.initprototype.ArgParser.Config

import java.io.File
import scala.util.{Failure, Success, Try}

object MainV13 {

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

    val tempDirectoryPath = FileUtils.getTempDirectory.toString
    gitClone(tempDirectoryPath, config, credentials.token)
    val localRepoPath     = new File(tempDirectoryPath).toPath.resolve(config.targetRepoName)

    val localPrototypeKitPath = new File(tempDirectoryPath).toPath.resolve("govuk-prototype-kit")
    localPrototypeKitPath.toFile.mkdir()
    %('npx, "govuk-prototype-kit", "create")(Path(localPrototypeKitPath))

    val originalLocalRepoFiles = localRepoPath.toFile.listFiles()
    localPrototypeKitPath.toFile.listFiles().foreach(file => println(file.getName))

    // Filter the git information from the prototype kit source before copying to local repo,
    // otherwise it will cause issues when pushing to remote destination repo
    val gitIgnoreFilter = FileFilterUtils.notFileFilter(
      FileFilterUtils.nameFileFilter(""".gitignore""")
    )

    FileUtils.copyDirectory(localPrototypeKitPath.toFile, localRepoPath.toFile, gitIgnoreFilter)

    val updatedLocalRepoFiles = localRepoPath.toFile.listFiles()

    if (updatedLocalRepoFiles.length > originalLocalRepoFiles.length) {
      gitPush(localRepoPath.toString, config)
    } else {
      throw new RuntimeException(
        s"Expected more than ${originalLocalRepoFiles.length} but found ${updatedLocalRepoFiles.length} in $localRepoPath"
      )
    }
  }

}
