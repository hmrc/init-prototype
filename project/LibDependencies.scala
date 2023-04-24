import sbt._

object LibDependencies {

  val compile = Seq(
    "com.github.scopt"           %% "scopt"           % "3.5.0",
    "org.zeroturnaround"          % "zt-zip"          % "1.14",
    "ch.qos.logback"              % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging"   % "3.5.0",
    "commons-io"                  % "commons-io"      % "2.7",
    "org.scalaj"                 %% "scalaj-http"     % "2.3.0",
    "com.typesafe.play"          %% "play-json"       % "2.9.0",
    "com.typesafe"                % "config"          % "1.4.0",
    "com.lihaoyi"                %% "requests"        % "0.6.9",
    "com.lihaoyi"                %% "os-lib"          % "0.7.8"
  )

  val test = Seq(
    "org.scalatest"         %% "scalatest"    % "3.2.0"   % "test",
    "org.scalatestplus"     %% "mockito-3-3"  % "3.2.0.0" % "test",
    "com.github.tomakehurst" % "wiremock"     % "2.18.0"  % "test",
    "com.vladsch.flexmark"   % "flexmark-all" % "0.35.10" % "test"
  )
}
