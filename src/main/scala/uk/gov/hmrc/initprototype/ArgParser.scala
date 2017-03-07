/*
 * Copyright 2016 HM Revenue & Customs
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

object ArgParser {



  val DEFAULT_BOOTSTRAP_TAG = "0.1.0"

  case class Config(credentialsFile: String = "",
                    githubHost: String = "",
                    org: String = "",
                    templateRepoName: String = "",
                    targetRepoName: String = "",
                    verbose: Boolean = false)


  val currentVersion = Option(getClass.getPackage.getImplementationVersion).getOrElse("(version not found)")

  val parser = new scopt.OptionParser[Config]("init-repository") {

    override def showUsageOnError = true

    head(s"\nInit-Repository", s"$currentVersion\n")

    help("help") text "prints this usage text"

    opt[String]("cred-file") action { (x, c) =>
      c.copy(credentialsFile = x)
    } text "github credentials file path"

    opt[String]("github-host") action { (x, c) =>
      c.copy(githubHost = x)
    } text "github's REST api base url"

    opt[String]("git-org") action { (x, c) =>
      c.copy(org = x)
    } text "github's org name"

    opt[String]("template-repo-name") action { (x, c) =>
      c.copy(templateRepoName = x)
    } text "the name of the template github repository"

    opt[String]("target-repo-name") action { (x, c) =>
      c.copy(targetRepoName = x)
    } text "the name of the target github repository"

    opt[Unit]('v', "verbose") action { (x, c) =>
      c.copy(verbose = true)
    } text "verbose mode (debug logging)"
  }
}
