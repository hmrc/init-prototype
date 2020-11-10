import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly._

object AssemblySettings {
  def apply() = Seq(
    assemblyJarName in assembly := "init-prototype.jar",
    mainClass in assembly := Some("uk.gov.hmrc.initprototype.Main"),
    assemblyMergeStrategy in assembly := {
      case "module-info.class"                                      => MergeStrategy.discard
      case PathList("org", "apache", "commons", "logging", xs @ _*) => MergeStrategy.first
      case PathList("play", "core", "server", xs @ _*)              => MergeStrategy.first
      case x                                                        =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.withClassifier(Some("assembly"))
    }
  )
}
