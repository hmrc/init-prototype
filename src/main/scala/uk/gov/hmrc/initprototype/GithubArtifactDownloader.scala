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

import java.io.{File, FileFilter}

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.zeroturnaround.zip.ZipUtil
import uk.gov.hmrc.initprototype.Main.logger
import scalaj.http.{Http, HttpOptions, HttpResponse}

class GithubArtifactDownloader() {

  def getRepoZipAndExplode(githubZipUri: String, artifactDownloadPath: String): String = {

    FileUtils.deleteDirectory(new File(artifactDownloadPath))
    getZipBallFromGithub(githubZipUri, artifactDownloadPath)
    logger.debug(s"saved zip ball to: $artifactDownloadPath")

    val file = new File(artifactDownloadPath)
    logger.debug(s"${file.getTotalSpace}")
    ZipUtil.explode(file)
    logger.debug(s"Zip file exploded successfully")
    getExplodedRootPath(artifactDownloadPath)
  }

  private def getExplodedRootPath(artifactDownloadPath: String) = {
    val file      = new File(artifactDownloadPath)
    val listFiles = file.listFiles(FileFilterUtils.directoryFileFilter().asInstanceOf[FileFilter])
    logger.debug("Dirs found:")
    logger.debug(listFiles.toList.mkString("\n"))

    logger.debug(s"Template repo will be created from: ${listFiles.headOption}")
    s"${listFiles.head}"
  }

  private def getZipBallFromGithub(githubZipUri: String, artifactDownloadPath: String) = {
    logger.debug(s"Getting code archive from: $githubZipUri")

    val bs: HttpResponse[Array[Byte]] = Http(githubZipUri)
      .header("content-type", "application/json")
      .option(HttpOptions.followRedirects(true))
      .asBytes

    logger.debug(s"Response code: ${bs.code}")
    if (bs.isError) {
      logger.error(
        s"Looks like we have encountered an error downloading the zip file from github:\n${new String(bs.body)}"
      )
      System.exit(-1)
    }
    logger.debug(s"Got ${bs.body.length} bytes from $githubZipUri... saving it to $artifactDownloadPath")

    val file = new File(artifactDownloadPath)
//    FileUtils.deleteQuietly(file)
    FileUtils.writeByteArrayToFile(file, bs.body)
    logger.debug(s"Saved file: $artifactDownloadPath")
  }

}
