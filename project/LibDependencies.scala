import sbt._

object LibDependencies {

  val compile = Seq(
    "com.github.scopt"           %% "scopt"           % "4.1.0",
    "ch.qos.logback"              % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.5",
    "commons-io"                  % "commons-io"      % "2.7",
    "com.typesafe.play"          %% "play-json"       % "2.9.0",
    "com.typesafe"                % "config"          % "1.4.0",
    "com.lihaoyi"                %% "os-lib"          % "0.7.8"
  )
}
