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

import java.io.PrintWriter
import java.io.File

import scala.concurrent.{Await, ExecutionContext, Future}

class HerokuReportTask(herokuManager: HerokuConnector, herokuConfiguration: HerokuConfiguration)(implicit
  executionContext: ExecutionContext
) {
  import herokuManager._

  def getAppReleaseInfo(appName: String): Future[String] = {
    val releasesFuture  = getAppReleases(appName, range = None)
    val formationFuture = getAppFormation(appName)

    for (
      (releases, _)   <- releasesFuture;
      formationOption <- formationFuture
    ) yield {
      val HerokuFormation(size, quantity, _) = formationOption.getOrElse(HerokuFormation("", 0, HerokuApp(appName)))

      val userReleases     =
        releases.filterNot(release => herokuConfiguration.administratorEmails.contains(release.userEmail))
      val created          = userReleases.head.createdAt
      val lastUpdated      = userReleases.last.createdAt
      val numberOfReleases = userReleases.size

      s"$appName\t$quantity\t$size\t$numberOfReleases\t$created\t$lastUpdated"
    }
  }

  def getAppsReleases: Future[Seq[String]] = getAppNames.flatMap { apps =>
    val headerRow = Future.successful("name\tnumberOfUnits\tdynoSize\tnumberOfReleases\tcreated\tlastUpdated")

    Future.sequence(headerRow +: apps.map(getAppReleaseInfo))
  }

  def getAppsReleases(args: Seq[String]): Future[Unit] = {
    if (args.length < 1) {
      throw new Exception("Missing path to apps file")
    }
    val reportFilePath = args.head
    getAppsReleases.map { apps =>
      val reportWriter = new PrintWriter(new File(reportFilePath))

      try for (app <- apps)
        reportWriter.println(app)
      finally reportWriter.close()
    }
  }
}

object HerokuReportTask extends App {
  implicit val ec: ExecutionContext            = ExecutionContext.global
  val herokuConfiguration: HerokuConfiguration = new HerokuConfiguration
  val herokuManager: HerokuConnector           = new HerokuConnector(herokuConfiguration)
  val herokuTask                               = new HerokuReportTask(herokuManager, herokuConfiguration)

  Await.result(herokuTask.getAppsReleases(args), herokuConfiguration.jobTimeout)
}
