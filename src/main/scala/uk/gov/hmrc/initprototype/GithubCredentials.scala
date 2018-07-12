/*
 * Copyright 2018 HM Revenue & Customs
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

import java.io.File
import java.nio.file.Path

import scala.io.Source

case class GithubCredentials(user:String, token:String)

object GithubCredentials {

  def apply(credentialFile :String): GithubCredentials = {
    val githubCredsOpt = findGithubCredsInFile(new File(credentialFile).toPath)
    githubCredsOpt.getOrElse(throw new scala.IllegalArgumentException(s"Did not find valid Github credentials in $credentialFile"))
  }

  def findGithubCredsInFile(file:Path):Option[GithubCredentials] = {
    val conf = new ConfigFile(file)

    conf.get("token") map { t => GithubCredentials("token", t)}
  }
}



class ConfigFile(file: Path) {

  private val kvMap: Map[String, String] = {
    try {
      Source.fromFile(file.toFile)
        .getLines().toSeq
        .map(_.split("="))
        .map { case Array(key, value) => key.trim -> value.trim}.toMap
    } catch {
      case e: Exception => {
        println(s"error parsing $file ${e.getMessage}")
        Map.empty
      }
    }
  }

  def get(path: String) = kvMap.get(path)
}
