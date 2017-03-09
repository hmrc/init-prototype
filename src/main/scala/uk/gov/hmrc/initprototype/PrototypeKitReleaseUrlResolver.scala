package uk.gov.hmrc.initprototype

import play.api.libs.json.Json

import scalaj.http.{Http, HttpResponse}

object PrototypeKitReleaseUrlResolver {


  def getLatestZipballUrl(repoApiUrl: String): Either [String , String] = {
    require(!repoApiUrl.endsWith("/"), s"repository api url should not end '/': $repoApiUrl")

    val latestReleaseUrl = s"$repoApiUrl/releases/latest"
    val response: HttpResponse[String] = Http(latestReleaseUrl).header("content-type" , "application/json").asString

    val responseBody = response.body
    if(response.isNotError) {

      val jsonFieldName = "zipball_url"
      (Json.parse(responseBody) \ jsonFieldName).asOpt[String] match {
        case Some(v) => Right(v)
        case None => Left(s"'$jsonFieldName' is not found in json response: ${Json.prettyPrint(Json.parse(responseBody))}")
      }
    } else {
      Left(s"HTTP error (${response.code}) while getting the release zip artifact from $latestReleaseUrl: $responseBody")
    }
  }
}
