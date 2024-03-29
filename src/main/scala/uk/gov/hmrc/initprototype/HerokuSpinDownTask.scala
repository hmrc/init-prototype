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

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source

class HerokuSpinDownTask(herokuManager: HerokuConnector)(implicit ec: ExecutionContext) {

  def spinDownApps(apps: Seq[String]): Future[Seq[HerokuFormation]] = Future.sequence(
    apps.map(herokuManager.spinDownApp)
  )

  def spinDownAppsFromFile(appsFile: String): Future[Seq[HerokuFormation]] = {
    val apps = Source.fromFile(appsFile, "UTF-8")
    try spinDownApps(apps.getLines.toSeq)
    finally apps.close()
  }

  def spinDownAppsFromFiles(appsFiles: Seq[String]): Future[Seq[Seq[HerokuFormation]]] = Future.sequence(
    appsFiles.map(spinDownAppsFromFile)
  )
}

object HerokuSpinDownTask extends App {
  implicit val ec: ExecutionContext            = ExecutionContext.global
  val herokuConfiguration: HerokuConfiguration = new HerokuConfiguration
  val herokuManager: HerokuConnector           = new HerokuConnector(herokuConfiguration)
  val herokuTask                               = new HerokuSpinDownTask(herokuManager)

  Await.result(herokuTask.spinDownAppsFromFiles(args), herokuConfiguration.jobTimeout)
}
