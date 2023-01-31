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
import org.slf4j
import org.slf4j.LoggerFactory
import uk.gov.hmrc.initprototype.ArgParser.Config

import java.io.File
import java.nio.file
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
    //FileUtils.getTempDirectory.toPath
    gitClone(tempDirectoryPath, config, credentials.token)
    val localRepoPath     = new File(tempDirectoryPath).toPath.resolve(config.targetRepoName)

    val localPrototypeKitPath: file.Path = new File(tempDirectoryPath).toPath.resolve("govuk-prototype-kit")
    localPrototypeKitPath.toFile.mkdir()
    %('npx, "govuk-prototype-kit", "create")(Path(localPrototypeKitPath))

    println(s"localPrototypeKitPath: $localPrototypeKitPath")
    println(s"localRepoPath: $localRepoPath")

    val localPrototypeKitPathSize = localPrototypeKitPath.toFile.listFiles().length
    val localRepoPathSize         = localRepoPath.toFile.listFiles().length

    println(s"localRepoPath size: $localRepoPathSize")
    println(s"localPrototypeKitPath size: $localPrototypeKitPathSize")

    FileUtils.copyDirectory(localPrototypeKitPath.toFile, localRepoPath.toFile)

//    val expectedLocalRepoPathSize = localPrototypeKitPathSize + localRepoPathSize
//    val actualLocalRepoPathSize   = localRepoPath.toFile.listFiles().length

    val updatedLocalRepoFiles = localRepoPath.toFile.listFiles()
    println("updatedLocalRepoFiles: ")
    updatedLocalRepoFiles.toList.foreach(println(_))

    if (updatedLocalRepoFiles.length > localRepoPathSize) {
      gitPush(localRepoPath.toString, config)
    } else {
      throw new RuntimeException(
        s"Expected more than $localRepoPathSize but found ${updatedLocalRepoFiles.length} in $localRepoPath"
      )
    }
  }

}
