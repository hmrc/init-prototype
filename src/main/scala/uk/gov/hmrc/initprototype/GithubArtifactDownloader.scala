package uk.gov.hmrc.initprototype

import java.io.{File, FileFilter}

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.zeroturnaround.zip.ZipUtil
import uk.gov.hmrc.initprototype.Main.logger

import scala.language.postfixOps
import scalaj.http.{Http, HttpOptions, HttpResponse}

object GithubArtifactDownloader {

  val downloadZipAs: String = s"/tmp/prototype-template-archive.zip"

  def getRepoZipAndExplode(githubZipUri: String, credentials: GithubCredentials): String = {

    getZipBallFromGithub(githubZipUri, credentials)
    logger.debug(s"saved zip ball to: $downloadZipAs")

    val file = new File(downloadZipAs)
    logger.debug(s"${file.getTotalSpace}")
    ZipUtil.explode(file)
    logger.debug(s"Zip file exploded successfully")
    getExplodedRootPath()
  }


  private def getExplodedRootPath() = {
    val file = new File(downloadZipAs)
    val listFiles = file.listFiles(FileFilterUtils.directoryFileFilter().asInstanceOf[FileFilter])
    logger.debug("Dirs found:")
    logger.debug(listFiles.toList.mkString("\n"))

    logger.debug(s"Template repo will be created from: ${listFiles.headOption}")
    s"${listFiles.head}"
  }


  private def getZipBallFromGithub(githubZipUri: String, credentials: GithubCredentials) = {
    logger.debug(s"Getting code archive from: $githubZipUri")

    val bs: HttpResponse[Array[Byte]] = Http(githubZipUri)
//      .auth(credentials.user, credentials.token)
      .header("Authorization", s"token ${credentials.token}")
      .header("content-type" , "application/json")
      .option(HttpOptions.followRedirects(true))
      .asBytes

    logger.debug(s"Response code: ${bs.code}")
    if (bs.isError) {
      logger.error(s"Looks like we have encountered an error downloading the zip file from github:\n${new String(bs.body)}")
      System.exit(-1)
    }
    logger.debug(s"Got ${bs.body.size} bytes from $githubZipUri... saving it to $downloadZipAs")

    val file = new File(downloadZipAs)
    FileUtils.deleteQuietly(file)
    FileUtils.writeByteArrayToFile(file, bs.body)
    logger.debug(s"Saved file: $downloadZipAs")
  }




}
