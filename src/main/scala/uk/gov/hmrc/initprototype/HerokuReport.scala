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

import play.api.libs.json.JsObject

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class HerokuReport(implicit herokuManager: HerokuManager, executionContext: ExecutionContext) {
  import herokuManager._

  def getAppReleaseInfo(appName: String): Future[String] =
    Future.sequence(Seq(getAppReleasesRecursive(appName), getAppQuantityAndSize(appName))).map { info =>
      val Seq((releases: Seq[JsObject], _), formation: Option[(Int, String)]) = info

      val (size, quantity) = formation.getOrElse((0, ""))

      val created     = (releases.head \ "created_at").as[String]
      val lastUpdated = (releases.last \ "created_at").as[String]

      s"$appName\t$quantity\t$size\t${releases.size}\t$created\t$lastUpdated"
    }

  def getAppsReleases(): Future[Seq[String]] = getAppNames.flatMap { apps =>
    val appsWithReleasesFutures = apps.map(getAppReleaseInfo)
    Future.sequence(appsWithReleasesFutures)
  }
}

object HerokuReport extends App {
  implicit val ec: ExecutionContext                     = ExecutionContext.global
  implicit val herokuConfiguration: HerokuConfiguration = new HerokuConfiguration
  implicit val herokuManager: HerokuManager             = new HerokuManager

  val herokuReport = new HerokuReport
  val apps         = Await.result(herokuReport.getAppsReleases, 60 seconds)

  for (app <- apps) {
    println(app)
  }
}
