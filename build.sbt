lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.1",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "core",
    fork := true,
    mainClass in Compile := Some("com.archmage.jobsheetmaker.MainApp"),
    libraryDependencies += "com.opencsv" % "opencsv" % "3.8",
    libraryDependencies += "org.apache.pdfbox" % "pdfbox" % "2.0.4"
  )