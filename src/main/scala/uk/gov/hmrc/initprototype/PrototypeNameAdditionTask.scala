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

import os.{proc, pwd}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.initprototype.Main.gitPush

import java.io.{BufferedWriter, File, FileInputStream, FileWriter}
import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.{Source, StdIn}
import scala.util.Try

case class PrototypeNameAdditionTask(githubRepositoryDownloader: GithubRepositoryDownloader) {

  private val prototypesAsJson = "prototypes-from-catalogue.json"
  private val downloadDirName  = "downloaded-prototypes"
  private val downloadDir      = new File(downloadDirName)
  private val pushedToMain     = new File("pushed-to-main.txt")
  private val pushedToBranch   = new File("pushed-to-branch.txt")

  def downloadPrototypes(): Future[Unit] = Future {
    val prototypes = prototypeDetailsFromJson()
    if (ensureDownloadDir()) {
      prototypes.map { prototypeRepo =>
        githubRepositoryDownloader.checkoutRepository(prototypeRepo.name, downloadDirName)
        val repositoryYamlFile = new File(s"$downloadDirName/${prototypeRepo.name}/repository.yaml")
        generateMissingPrototypeName(
          yamlFileToMap(repositoryYamlFile),
          prototypeRepo.name
        ) map { generatedName =>
          updateAndCommitYaml(repositoryYamlFile, prototypeRepo.name, generatedName)
        }
      }
    }
  }

  private def prototypeDetailsFromJson(): Seq[PrototypeDetails] = {
    val stream = new FileInputStream(prototypesAsJson)
    val json   =
      try Json.parse(stream)
      finally stream.close()
    json.validate[Seq[PrototypeDetails]].getOrElse {
      println(s"Could not validate $prototypesAsJson as prototype details")
      Seq()
    }
  }

  private def ensureDownloadDir(): Boolean =
    if (downloadDir.exists() && downloadDir.isDirectory) true else downloadDir.mkdirs()

  private def yamlFileToMap(repositoryYaml: File): Map[String, String] = {
    val source = Source.fromFile(repositoryYaml)
    val asMap  = source
      .getLines()
      .map { line =>
        val keyAndValue = line.split(":").toList
        (keyAndValue.head.trim, keyAndValue.tail.mkString(":").trim)
      }
      .toMap
    source.close()
    asMap
  }

  private def stripSuffixFromName(prototypeNameFromUrl: String): String = {
    val wordsFromName = prototypeNameFromUrl.split("-")
    if (wordsFromName.last.length == 12 && wordsFromName.last.exists(_.isDigit)) {
      wordsFromName.dropRight(1).mkString("-")
    } else {
      prototypeNameFromUrl
    }
  }

  private def generateMissingPrototypeName(
    yamlAsMap: Map[String, String],
    prototypeName: String
  ): Option[String] =
    if (yamlAsMap.isDefinedAt("prototype-auto-publish") && !yamlAsMap.isDefinedAt("prototype-name")) {
      yamlAsMap.get("prototype-url") match {
        case Some(urlAsString) =>
          val parsedUrl              = new URL(urlAsString.trim)
          val prototypeNameFromUrl   = parsedUrl.getHost.split("\\.").head
          val nameWithSuffixStripped = stripSuffixFromName(prototypeNameFromUrl)
          Some(nameWithSuffixStripped)
        case _                 => Some(prototypeName)
      }
    } else None

  private def updateAndCommitYaml(repositoryYamlFile: File, repoName: String, generatedName: String) = {
    println(s"$repoName contains prototype-auto-publish property")
    println(s"About to add to repo $repoName prototype-name of: $generatedName")
    println(s"Confirm write and push of repository.yaml: y to confirm")
    StdIn.readLine() match {
      case "y" =>
        writeAndCommit(repositoryYamlFile, generatedName, repoName, pushedToMain) getOrElse {
          val branchName = "PLATUI-2475_bulk-update-repository-yaml"
          proc("git", "checkout", "-b", branchName).call(pwd / downloadDirName / repoName)
          writeAndCommit(
            repositoryYamlFile,
            generatedName,
            repoName,
            pushedToBranch,
            "--set-upstream",
            "origin",
            branchName
          ) getOrElse {
            println(s"Could not update repo: $repoName")
          }
        }
      case _   =>
        println(s"Cancelling commit and push to: $repoName")
    }
  }

  def writeAndCommit(
    repositoryYamlFile: File,
    generatedName: String,
    repoName: String,
    outputFile: File,
    additionalArgs: String*
  ) = {
    val commitMsg = "PLATUI-2475: Bulk update adding prototype-name to repository.yaml file"
    Try {
      appendToFile(repositoryYamlFile, s"prototype-name: $generatedName")
      gitPush(pwd / downloadDirName / repoName, commitMsg, additionalArgs: _*)
    } map { _ =>
      appendToFile(outputFile, repoName)
    }
  }

  private def appendToFile(fileName: File, lineToAppend: String) = {
    val writer = new BufferedWriter(new FileWriter(fileName, true))
    writer.write(s"\n$lineToAppend")
    writer.close()
  }
}

case class PrototypeDetails(name: String, url: String, isArchived: Boolean)

object PrototypeDetails {
  implicit val fmts: Format[PrototypeDetails] = Json.format[PrototypeDetails]
}

object PrototypeNameAdditionTask extends App {
  val githubRepositoryDownloader = new GithubRepositoryDownloader()
  val prototypeNameAdditionTask  = new PrototypeNameAdditionTask(
    githubRepositoryDownloader
  )

  Await.result(prototypeNameAdditionTask.downloadPrototypes(), Duration.Inf)
}
