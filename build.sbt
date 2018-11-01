
val libName = "init-prototype"

lazy val InitPrototype = Project(libName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 0,
    makePublicallyAvailableOnBintray := true,
    scalaVersion := "2.11.8",
    libraryDependencies ++= LibDependencies.compile ++ LibDependencies.test,
    resolvers += Resolver.typesafeRepo("releases"),
    AssemblySettings(),
    parallelExecution := false,
    addArtifact(artifact in (Compile, assembly), assembly)
  )


