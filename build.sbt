name := "SmallWindowsServiceLibrary"

version := "0.1"

scalaVersion := "2.11.4"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"

// disable using the Scala version in output paths and artifacts
crossPaths := false

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  "swsl" + "_" + module.revision + "." + artifact.extension
}
