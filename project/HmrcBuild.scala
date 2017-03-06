
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.versioning.SbtGitVersioning


object HmrcBuild extends Build {

  import BuildDependencies._
  import uk.gov.hmrc.DefaultBuildSettings._

  val appName = "init-prototype"

  val libraries = Seq(
    "com.github.scopt" %% "scopt" % "3.5.0",
    "org.zeroturnaround" % "zt-zip" % "1.10",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "commons-io" % "commons-io" % "2.5",
    "org.scalaj" %% "scalaj-http" % "2.3.0",
    "com.lihaoyi" %% "ammonite-ops" % "0.8.2"


    //    "com.typesafe.play" %% "play-ws" % "2.4.3",
//    "commons-io" % "commons-io" % "2.4",
//    "org.apache.httpcomponents" % "httpcore" % "4.3.2",
//    "org.apache.httpcomponents" % "httpclient" % "4.3.5",
//    "org.scalatest" %% "scalatest" % "2.2.2" % "test",
//    "org.pegdown" % "pegdown" % "1.4.2" % "test",
//    "org.mockito" % "mockito-all" % "1.9.5" % "test",
//    "com.github.tomakehurst" % "wiremock" % "1.52" % "test"
  )

  lazy val InitPrototype = Project(appName, file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(
      scalaVersion := "2.11.8",
      libraryDependencies ++= libraries,
      resolvers += Resolver.typesafeRepo("releases"),
      BuildDescriptionSettings(),
      AssemblySettings(),
      parallelExecution := false,
      addArtifact(artifact in (Compile, assembly), assembly)
    )
}

object AssemblySettings{
  def apply()= Seq(
    assemblyJarName in assembly := "init-prototype.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", "commons", "logging", xs@_*) => MergeStrategy.first
      case PathList("play", "core", "server", xs@_*) => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    artifact in(Compile, assembly) := {
      val art = (artifact in(Compile, assembly)).value
      art.copy(`classifier` = Some("assembly"))
    }
  )
}


  object BuildDescriptionSettings {

    def apply() =
      pomExtra := <url>https://www.gov.uk/government/organisations/hm-revenue-customs</url>
        <licenses>
          <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          </license>
        </licenses>
        <scm>
          <connection>scm:git@github.com:hmrc/releaser.git</connection>
          <developerConnection>scm:git@github.com:hmrc/releaser.git</developerConnection>
          <url>git@github.com:hmrc/releaser.git</url>
        </scm>
        <developers>
          <developer>
            <id>arminio</id>
            <name>Armin Keyvanloo</name>
            <url>http://www.equalexperts.com</url>
          </developer>
          <developer>
            <id>stevesmith</id>
            <name>Steve Smith</name>
            <url>http://www.equalexperts.com</url>
          </developer>
        </developers>
  }
