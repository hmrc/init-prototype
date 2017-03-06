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

import java.io.{File, FileFilter}

import ch.qos.logback.classic.{Level, Logger}
import org.apache.commons.io.filefilter.FileFilterUtils
import org.slf4j
import org.slf4j.LoggerFactory
import uk.gov.hmrc.initprototype.ArgParser.Config

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import ammonite.ops.{%, _}
import ammonite.ops.ImplicitWd._

object Main {


  val logger = com.typesafe.scalalogging.Logger("init-prototype")

//  def buildGithub(credentialsFile: String, apiBaseUrl: String, org: String) = {
//    new Github(
//      new GithubHttp(ServiceCredentials(credentialsFile)),
//      new GithubUrls(apiBaseUrl, org)
//    )
//  }

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


  def gitInitOrg(localRepoPath: String, repoName: String, token: String) = {
    logger.debug(s"$localRepoPath")
    val dir = Path(localRepoPath)
    %('git, "init" , ".")(dir)
    %('git, "add", ".", "-A")(dir)
    %('git, "commit" , "-m", s"Creating new prototype $repoName")(dir)
    %('git, "remote" , "add", "origin", s"https://$token:x-oauth-basic@github.tools.tax.service.gov.uk/HMRC/$repoName.git")(dir)
    %('git, "push" , "--set-upstream", "origin", "master")(dir)
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


//  def gitInitMonad(localRepoPath: String, repoName: String, token: String) = {
//    logger.debug(s"$localRepoPath")
//    val dir = Path(localRepoPath)
//
//
//    val x = for {
//      a <- %%('git, "init" , ".")(dir)
//      b <- %%('git, "add", ".", "-A")(dir).chunks
//      c <- %%('git, "commit" , "-m", s"Creating new prototype $repoName")(dir).chunks
//      d <- %%('git, "remote" , "add", "origin", s"https://$token:x-oauth-basic@github.tools.tax.service.gov.uk/HMRC/$repoName.git")(dir).chunks
//      e <- %%('git, "push" , "--set-upstream", "origin", "master")(dir).chunks
//    } yield ()
//
//  }





  def start(config: Config): Unit = {
    val credentials = GithubCredentials(config.credentialsFile)

    val githubZipUri = getZipBallArtifactUrl(config)
    val localRepoPath = GithubArtifactDownloader.getRepoZipAndExplode(githubZipUri, credentials)
    gitInit(localRepoPath, config.targetRepoName, credentials.token)
    

//    val github = buildGithub(config.credentialsFile, config.gitApiBaseUrl, config.org)
//    val webHookCreateConfig = WebHookCreateConfig(config.webhookUrl, config.webhookSecret)

//    try {
//
//      val createHooksFuture = Future.sequence(
//        config.repoNames.map(repoName => github.tryCreateWebhook(repoName, webHookCreateConfig, config.events))
//      )
//
//      createHooksFuture.map(_.filter(_.isFailure)).map { failures =>
//        val failedMessages: Seq[String] = failures.collect { case Failure(t) => t.getMessage }
//        if (failedMessages.nonEmpty) {
//          val errorMessage =
//            "########### Failure while creating some repository hooks, please see previous errors ############\n" + failedMessages.mkString("\n")
//
//          throw new RuntimeException(errorMessage)
//        }
//      }.await
//
//    } finally {
//      github.close()
//    }
  }






}
