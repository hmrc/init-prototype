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

import java.io.PrintWriter
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.{Success, Try}

case class PackageLockReportTask(
  herokuManager: HerokuConnector,
  githubManager: GithubConnector,
  conf: PackageLockReportConfiguration
) {

  def generateReport(): Future[Unit] = Future {
    val herokuPrototypes = parseHerokuUsageReport()
    val enrichedRows     = resolveGithubRepos(herokuPrototypes)
    writeRowsToFile(enrichedRows)
  }

  private def parseHerokuUsageReport(): List[ReportRow] =
    Try(Source.fromFile((os.pwd / conf.herokuUsageReportFile).toString())) match {
      case Success(source) =>
        val linesWithoutHeader = source.getLines().toList.tail
        val herokuPrototypes   = linesWithoutHeader.flatMap(parseHerokuUsageReportRow)
        source.close()
        herokuPrototypes
      case _               =>
        println(
          s"Unable to open ${conf.herokuUsageReportFile} file from current directory. Have you run the the generateHerokuReport task?"
        )
        Nil
    }

  private def parseHerokuUsageReportRow(input: String): Option[ReportRow] =
    Try {
      val name              = input.split("\t").head
      val numberOfInstances = input.split("\t")(3).toInt
      ReportRow(
        prototypeName = name,
        repoName = None,
        containsPackageJson = None,
        containsPackageLockJson = None,
        deployedToHeroku = Some(true),
        runningInHeroku = Some(numberOfInstances > 0),
        inGithub = None
      )
    } match {
      case Success(value) => Some(value)
      case _              =>
        println(s"Couldn't parse as ReportRow: $input")
        None
    }

  private def resolveGithubRepos(herokuPrototypes: List[ReportRow]): List[ReportRow] =
    herokuPrototypes.map { herokuData =>
      val githubRepoExistsWithSameName = githubManager.repoExists(herokuData.prototypeName)
      val gitHubRepo                   = if (githubRepoExistsWithSameName) {
        Some(herokuData.prototypeName)
      } else {
        getGithubRepoFromHerokuApp(herokuData.prototypeName)
      }

      gitHubRepo match {
        case None       => herokuData
        case Some(repo) =>
          val hasPackageJson     = githubManager.fileExists("package.json", repo)
          val hasPackageLockJson = githubManager.fileExists("package-lock.json", repo)
          herokuData.copy(
            repoName = gitHubRepo,
            containsPackageJson = Some(hasPackageJson),
            containsPackageLockJson = Some(hasPackageLockJson),
            inGithub = Some(true)
          )
      }
    }

  def getGithubRepoFromHerokuApp(appName: String): Option[String] = {
    val eventualRepoName = for {
      slugIds <- herokuManager.getSlugIds(appName)
      repoName = getNameFromSlugIds(appName, slugIds)
    } yield repoName
    Await.result(eventualRepoName, Duration.Inf)
  }

  private def getNameFromSlugIds(appName: String, slugIds: Seq[String]): Option[String] = {
    @tailrec
    def getNameFromId(ids: Seq[String]): Option[String] = ids.toList match {
      case Nil          => None
      case head :: tail =>
        Await.result(herokuManager.getCommitId(appName, head), Duration.Inf) match {
          case Some(commitSha) =>
            githubManager.getGithubRepoName(commitSha) match {
              case Some(repoName) => Some(repoName)
              case None           => getNameFromId(tail)
            }
          case None            => getNameFromId(tail)
        }
    }

    getNameFromId(slugIds.distinct)
  }

  private def writeRowsToFile(rows: List[ReportRow]): Unit = {
    val printWriter     = new PrintWriter((os.pwd / conf.packageLockReportFile).toString())
    val headerRow       = List(
      "prototypeName",
      "githubRepoName",
      "containsPackageJson",
      "containsPackageLockJson",
      "deployedToHeroku",
      "runningInHeroku",
      "inGitHub"
    ).mkString("\t")
    val allRowsAsString = (List(headerRow) ++ rows.map(_.toString)).mkString("\n")
    printWriter.print(allRowsAsString)
    printWriter.close()
  }
}

case class ReportRow(
  prototypeName: String,
  repoName: Option[String],
  containsPackageJson: Option[Boolean],
  containsPackageLockJson: Option[Boolean],
  deployedToHeroku: Option[Boolean],
  runningInHeroku: Option[Boolean],
  inGithub: Option[Boolean]
) {

  override def toString: String =
    List(
      prototypeName,
      repoName.getOrElse("unknown"),
      getOrDefault(containsPackageJson),
      getOrDefault(containsPackageLockJson),
      getOrDefault(deployedToHeroku),
      getOrDefault(runningInHeroku),
      getOrDefault(inGithub)
    ).mkString("\t")

  private def getOrDefault(optBool: Option[Boolean]): String = {
    val default = "unknown"
    optBool.map(_.toString).getOrElse(default)
  }
}

object PackageLockReportTask extends App {
  val herokuManager: HerokuConnector = new HerokuConnector(new HerokuConfiguration)
  val githubManager: GithubConnector = new GithubConnector(new GithubConfiguration)
  val packageLockReportTask          = new PackageLockReportTask(herokuManager, githubManager, PackageLockReportConfiguration())

  Await.result(packageLockReportTask.generateReport(), Duration.Inf)
}
