/*
 * Copyright 2022 HM Revenue & Customs
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

import java.io.{File, PrintWriter}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.util.{Success, Try}

case class PackageLockReportTask(
  downloader: GithubRepositoryDownloader,
  conf: PackageLockReportConfiguration
) {

  def checkoutAndReport(args: Array[String]): Future[Unit] = Future {
    val directory        = ensurePrototypesDirectory(conf.prototypesDirectory)
    val herokuPrototypes = parseHerokuUsageReport()
    downloader.checkoutRepositories(herokuPrototypes.map(_.prototypeName), conf.prototypesDirectory)
    generatePackageLockReport(herokuPrototypes, directory)
  }

  private def ensurePrototypesDirectory(directoryName: String): File = {
    val directory = new File((os.pwd / directoryName).toString())
    if (!directory.exists()) directory.mkdir()
    directory
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

  private def parseLocalGithubRepos(repositories: List[File]): List[ReportRow] =
    repositories.flatMap { repository =>
      if (repository.isDirectory) {
        val repositoryContents: List[String] = repository.listFiles().toList.map(_.getName)
        Some(
          ReportRow(
            prototypeName = repository.getName,
            containsPackageJson = Some(repositoryContents.contains("package.json")),
            containsPackageLockJson = Some(repositoryContents.contains("package-lock.json")),
            deployedToHeroku = None,
            runningInHeroku = None,
            inGithub = Some(true)
          )
        )
      } else {
        None
      }
    }

  private def generatePackageLockReport(herokuPrototypes: List[ReportRow], directory: File): Unit = {
    val repositories: List[File] = directory.listFiles().toList
    val githubRepositories       = parseLocalGithubRepos(repositories)
    writeRowsToFile(combineLists(githubRepositories, herokuPrototypes))
  }

  private def combineLists(githubRepos: List[ReportRow], herokuPrototypes: List[ReportRow]): List[ReportRow] = {
    val updatedGithubRepos = githubRepos.map { githubRepo =>
      herokuPrototypes.find(_.prototypeName == githubRepo.prototypeName) match {
        case Some(matchingHerokuRow) =>
          githubRepo.copy(
            deployedToHeroku = Some(true),
            runningInHeroku = matchingHerokuRow.runningInHeroku
          )
        case _                       => githubRepo
      }
    }

    val remainingHerokuRows =
      herokuPrototypes.filterNot(hr => githubRepos.map(_.prototypeName).contains(hr.prototypeName))
    val updatedHerokuRows   = remainingHerokuRows.map(_.copy(inGithub = Some(false)))

    (updatedGithubRepos ++ updatedHerokuRows).sortBy(_.prototypeName)
  }

  private def writeRowsToFile(rows: List[ReportRow]) = {
    val printWriter     = new PrintWriter((os.pwd / conf.packageLockReportFile).toString())
    val headerRow       =
      s"prototypeName\tcontainsPackageJson\tcontainsPackageLockJson\tdeployedToHeroku\trunningInHeroku\tinGitHub"
    val allRowsAsString = (List(headerRow) ++ rows.map(_.toString)).mkString("\n")
    printWriter.print(allRowsAsString)
    printWriter.close()
  }
}

case class ReportRow(
  prototypeName: String,
  containsPackageJson: Option[Boolean],
  containsPackageLockJson: Option[Boolean],
  deployedToHeroku: Option[Boolean],
  runningInHeroku: Option[Boolean],
  inGithub: Option[Boolean]
) {

  override def toString: String =
    s"$prototypeName\t${getOrDefault(containsPackageJson)}\t${getOrDefault(containsPackageLockJson)}\t${getOrDefault(
      deployedToHeroku
    )}\t${getOrDefault(runningInHeroku)}\t${getOrDefault(inGithub)}"

  private def getOrDefault(optBool: Option[Boolean]): String = {
    val default = "unknown"
    optBool.map(_.toString).getOrElse(default)
  }
}

object PackageLockReportTask extends App {
  implicit val ec: ExecutionContext  = ExecutionContext.global
  val githubRepositoryDownloader     = new GithubRepositoryDownloader()
  val packageLockReportConfiguration = new PackageLockReportConfiguration()
  val packageLockReportTask          = new PackageLockReportTask(githubRepositoryDownloader, packageLockReportConfiguration)

  Await.result(packageLockReportTask.checkoutAndReport(args), Duration.Inf)
}
