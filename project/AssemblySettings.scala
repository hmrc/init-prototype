import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly._

object AssemblySettings {
  def apply() = Seq(
    assembly / assemblyJarName := "init-prototype.jar",
    assembly / mainClass := Some("uk.gov.hmrc.initprototype.MainV13"),
    assembly / assemblyMergeStrategy := {
      case "module-info.class"                                      => MergeStrategy.discard
      case PathList("org", "apache", "commons", "logging", xs @ _*) => MergeStrategy.first
      case PathList("play", "core", "server", xs @ _*)              => MergeStrategy.first
      case x                                                        =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    Compile / assembly / artifact := {
      val art = (Compile / assembly / artifact).value
      art.withClassifier(Some("assembly"))
    }
  )
}
