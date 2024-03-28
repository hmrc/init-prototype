val libName = "init-prototype"

lazy val InitPrototype = Project(libName, file("."))
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    majorVersion := 0,
    isPublicArtefact := true,
    scalaVersion := "2.13.12",
    scalacOptions += "-deprecation",
    libraryDependencies ++= LibDependencies.compile,
    resolvers += Resolver.typesafeRepo("releases"),
    AssemblySettings(),
    addArtifact(Compile / assembly / artifact, assembly)
  )
