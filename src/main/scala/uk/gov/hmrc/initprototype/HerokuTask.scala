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

import play.api.libs.json.JsValue

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.concurrent.duration._

class HerokuTask(
  implicit herokuManager: HerokuManager,
  herokuConfiguration: HerokuConfiguration,
  ec: ExecutionContext) {

  def spinDownApps(apps: Seq[String]): Future[Seq[JsValue]] = Future.sequence(
    apps.map(herokuManager.spinDownApp)
  )

  def spinDownApps(appsFile: String): Future[Seq[JsValue]] = {
    val apps = Source.fromFile(appsFile, "UTF-8")
    try {
      spinDownApps(apps.getLines.toArray)
    } finally {
      apps.close()
    }
  }
}

object HerokuTask extends App {
  implicit val ec: ExecutionContext                     = ExecutionContext.global
  implicit val herokuConfiguration: HerokuConfiguration = new HerokuConfiguration
  implicit val herokuManager: HerokuManager             = new HerokuManager

  val herokuTask = new HerokuTask

  val futures: Seq[Future[Seq[JsValue]]] = args.map(herokuTask.spinDownApps)
  Await.result(Future.sequence(futures), 60 seconds)
}
