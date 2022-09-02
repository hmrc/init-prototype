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

import os.Path
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import java.time.Instant

class HerokuGenerateSpinDownListTask(config: HerokuConfiguration) {

  case class SpinDownCandidate(name: String, lastUpdated: Instant)
  implicit val spinDownCandidate: Reads[SpinDownCandidate] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "updated_at").read[Instant]
  )(SpinDownCandidate.apply _)

  def generateSpinDownList(outputPath: Path): Unit = {
    val newCandidates = herokuGet(s"${config.baseUrl}/teams/digital-hmrc-gov/apps")
      .as[List[SpinDownCandidate]]
      .filter(candidate =>
        candidate.lastUpdated.isBefore(Instant.now.minus(config.periodAfterWhichAppConsideredInactive))
          && !config.herokuAppsToKeepRunning.contains(candidate.name)
          && isCurrentlyRunning(candidate.name)
      )

    val spinDownList = if (os.exists(outputPath)) {
      val existingCandidates = os.read.lines(outputPath).toSet
      if (existingCandidates.nonEmpty) {
        println("""
          |IMPORTANT: EXISTING SPIN DOWN LIST FOUND
          |===============================================
          |
          |When an existing spin down list is found, this
          |task will update it to remove apps updated in
          |the last week or were added to the list of apps
          |to keep running. No apps that became inactive
          |this week will be added, because they would not
          |have received notice of the plan to spin down
          |their app.
          |
          |If you instead meant to plan a new spin down,
          |then remove the file and rerun this task.
          |""".stripMargin)
        newCandidates.filter(candidate => existingCandidates.contains(candidate.name))
      } else {
        newCandidates
      }
    } else newCandidates

    os.write.over(
      outputPath,
      spinDownList
        .map(_.name)
        .mkString("\n")
    )

    println(s"Spin down list: ${outputPath.toString()}")
  }

  private def isCurrentlyRunning(name: String): Boolean =
    (herokuGet(s"${config.baseUrl}/apps/$name/formation") \\ "quantity")
      .exists(quantity => quantity.as[Int] > 0)

  private def herokuGet(url: String): JsValue = Json.parse(
    requests
      .get(
        url,
        headers = Map(
          "Range"         -> "name ..; max=1000;",
          "Accept"        -> "application/vnd.heroku+json; version=3",
          "Authorization" -> s"Bearer ${config.apiToken}"
        )
      )
      .text
  )
}

object HerokuGenerateSpinDownListTask extends App {
  new HerokuGenerateSpinDownListTask(new HerokuConfiguration)
    .generateSpinDownList(os.pwd / "spin-down-list.txt")
}
