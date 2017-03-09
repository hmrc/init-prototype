package uk.gov.hmrc.initprototype

import java.io.{File, FileFilter}

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.zeroturnaround.zip.ZipUtil
import uk.gov.hmrc.initprototype.Main.logger

import scala.language.postfixOps
import scalaj.http.{Http, HttpOptions, HttpResponse}

class GithubArtifactDownloader(artifactDownloadPath: String ) {

  def getRepoZipAndExplode(githubZipUri: String): String = {

    getZipBallFromGithub(githubZipUri)
    logger.debug(s"saved zip ball to: $artifactDownloadPath")

    val file = new File(artifactDownloadPath)
    logger.debug(s"${file.getTotalSpace}")
    ZipUtil.explode(file)
    logger.debug(s"Zip file exploded successfully")
    getExplodedRootPath()
  }


  private def getExplodedRootPath() = {
    val file = new File(artifactDownloadPath)
    val listFiles = file.listFiles(FileFilterUtils.directoryFileFilter().asInstanceOf[FileFilter])
    logger.debug("Dirs found:")
    logger.debug(listFiles.toList.mkString("\n"))

    logger.debug(s"Template repo will be created from: ${listFiles.headOption}")
    s"${listFiles.head}"
  }


  private def getZipBallFromGithub(githubZipUri: String) = {
    logger.debug(s"Getting code archive from: $githubZipUri")

    val bs: HttpResponse[Array[Byte]] = Http(githubZipUri)
      .header("content-type" , "application/json")
      .option(HttpOptions.followRedirects(true))
      .asBytes

    logger.debug(s"Response code: ${bs.code}")
    if (bs.isError) {
      logger.error(s"Looks like we have encountered an error downloading the zip file from github:\n${new String(bs.body)}")
      System.exit(-1)
    }
    logger.debug(s"Got ${bs.body.length} bytes from $githubZipUri... saving it to $artifactDownloadPath")

    val file = new File(artifactDownloadPath)
    FileUtils.deleteQuietly(file)
    FileUtils.writeByteArrayToFile(file, bs.body)
    logger.debug(s"Saved file: $artifactDownloadPath")
  }




}
