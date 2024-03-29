val libName = "init-prototype"

lazy val InitPrototype = Project(libName, file("."))
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    majorVersion := 0,
    isPublicArtefact := true,
    scalaVersion := "2.12.10",
    scalacOptions += "-deprecation",
    libraryDependencies ++= LibDependencies.compile ++ LibDependencies.test,
    resolvers += Resolver.typesafeRepo("releases"),
    AssemblySettings(),
    parallelExecution := false,
    addArtifact(Compile / assembly / artifact, assembly)
  )

val spinDownHerokuApps = inputKey[Unit]("Spin down the heroku apps listed in the given file.")
fullRunInputTask(spinDownHerokuApps, Compile, "uk.gov.hmrc.initprototype.HerokuSpinDownTask")

val generateHerokuReport = inputKey[Unit]("Generate a usage report from Heroku.")
fullRunInputTask(generateHerokuReport, Compile, "uk.gov.hmrc.initprototype.HerokuReportTask")

val generatePackageLockReport = inputKey[Unit]("Generates a report of prototypes' package-lock.json")
fullRunInputTask(generatePackageLockReport, Compile, "uk.gov.hmrc.initprototype.PackageLockReportTask")

val generateSpinDownList =
  inputKey[Unit]("Generate a spin-down-list.txt of heroku apps that might not be being used and could be turned off")
fullRunInputTask(generateSpinDownList, Compile, "uk.gov.hmrc.initprototype.HerokuGenerateSpinDownListTask")

val updateRepositoryYaml =
  inputKey[Unit]("Update repository YAML files for prototypes to include prototype-name")
fullRunInputTask(updateRepositoryYaml, Compile, "uk.gov.hmrc.initprototype.PrototypeNameAdditionTask")
