import sbt._

object LibDependencies {

  val compile = Seq(
    "com.github.scopt" %% "scopt" % "3.5.0",
    "org.zeroturnaround" % "zt-zip" % "1.10",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "commons-io" % "commons-io" % "2.5",
    "org.scalaj" %% "scalaj-http" % "2.3.0",
    "com.lihaoyi" %% "ammonite-ops" % "0.8.2",
    "org.apache.commons" % "commons-io" % "1.3.2",
    "com.typesafe.play" %% "play-json" % "2.5.12"
  )

  val test = Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "com.github.tomakehurst" % "wiremock" % "1.52" % "test",
    "org.pegdown" % "pegdown" % "1.4.2" % "test"
  )

}