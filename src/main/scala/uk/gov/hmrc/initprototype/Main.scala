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

import ch.qos.logback.classic.{Level, Logger}
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils.{and, directoryFileFilter, nameFileFilter}
import org.apache.commons.io.filefilter.FileFilterUtils
import org.slf4j
import org.slf4j.LoggerFactory
import os.{Path, proc}
import uk.gov.hmrc.initprototype.ArgParser.Config

import java.io.File
import scala.util.{Failure, Success, Try}

object Main {

  val logger = com.typesafe.scalalogging.Logger("init-prototype")

  def main(args: Array[String]): Unit = {

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

  def gitClone(localRepoPath: String, config: Config) = {

    val repoUrl =
      s"https://${config.githubToken}:x-oauth-basic@${config.targetGithubHost}/${config.targetOrg}/${config.targetRepoName}.git"

    logger.debug(s"Cloning to: $localRepoPath")
    val dir = Path(localRepoPath)

    val repoPath = new File(localRepoPath).toPath.resolve(config.targetRepoName).toString
    proc("rm", "-rf", repoPath).call(dir)

    proc("git", "clone", repoUrl).call(dir)
  }

  def gitPush(dir: Path, commitMessage: String, additionalArgs: String*) = {
    proc("git", "add", ".", "-A").call(dir)
    proc("git", "commit", "-m", commitMessage).call(dir)

    logger.debug(s"Pushing: $dir")
    val tryOfPushResult: Try[os.CommandResult] = Try(proc("git", "push", additionalArgs).call(dir))

    tryOfPushResult match {
      case Success(pushResult) =>
        if (pushResult.exitCode != 0) {
          throw new RuntimeException(s"Unable to push to remote repo.")
        }
      case Failure(t)          => throw new RuntimeException(s"Unable to push to remote repo.")
    }

  }

  def start(config: Config): Unit = {
    val tempDirectoryPath = FileUtils.getTempDirectory.toString
    gitClone(tempDirectoryPath, config)
    val localRepoPath     = new File(tempDirectoryPath).toPath.resolve(config.targetRepoName)

    val localPrototypeKitPath = new File(tempDirectoryPath).toPath.resolve("govuk-prototype-kit")
    localPrototypeKitPath.toFile.mkdir()
    proc("npx", "govuk-prototype-kit", "create").call(Path(localPrototypeKitPath))

    // Filter the git information from the prototype kit source before copying to local repo,
    // otherwise it will cause issues when pushing to remote destination repo
    val gitDirFilter =
      FileFilterUtils.notFileFilter(and(directoryFileFilter, nameFileFilter(".git")))

    FileUtils.copyDirectory(localPrototypeKitPath.toFile, localRepoPath.toFile, gitDirFilter)
    gitPush(Path(localRepoPath.toString), s"Creating new prototype ${config.targetRepoName}")
  }

}
