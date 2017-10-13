lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.archmage",
      scalaVersion := "2.12.3",
      version      := "1.6"
    )),
    name := "JobsheetMaker",
    fork := true,
    mainClass in Compile := Some("com.archmage.jobsheetmaker.MainApp"),
    mainClass in assembly := Some("com.archmage.jobsheetmaker.MainApp"),
    resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases",
    libraryDependencies += "com.opencsv" % "opencsv" % "3.8",
    libraryDependencies += "org.apache.pdfbox" % "pdfbox" % "2.0.4",
    libraryDependencies += "org.apache.poi" % "poi" % "3.16",
    libraryDependencies += "org.apache.poi" % "poi-ooxml" % "3.16",
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
  )