val libName = "init-prototype"

lazy val InitPrototype = Project(libName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 0,
    makePublicallyAvailableOnBintray := true,
    scalaVersion := "2.12.10",
    scalacOptions += "-deprecation",
    libraryDependencies ++= LibDependencies.compile ++ LibDependencies.test,
    resolvers += Resolver.typesafeRepo("releases"),
    AssemblySettings(),
    parallelExecution := false,
    addArtifact(artifact in (Compile, assembly), assembly)
)

val spinDownHerokuApps = inputKey[Unit]("Spin down the heroku apps listed in the given file.")
fullRunInputTask(spinDownHerokuApps, Compile, "uk.gov.hmrc.initprototype.HerokuSpinDownTask")

val generateHerokuReport = inputKey[Unit]("Generate a usage report from Heroku.")
fullRunInputTask(generateHerokuReport, Compile, "uk.gov.hmrc.initprototype.HerokuReportTask")
