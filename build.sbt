val scala3Version = "3.3.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "gears-style",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "ch.epfl.lamp" %% "gears" % "0.1.0-SNAPSHOT",
      "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )
