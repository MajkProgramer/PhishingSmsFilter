import sbtassembly.AssemblyPlugin.autoImport._

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.16"

lazy val root = (project in file("."))
  .settings(
    name := "PhishingSmsFilter",
    scalacOptions += "-Ymacro-annotations",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % "0.23.17",
      "org.http4s" %% "http4s-circe" % "0.23.30",
      "org.http4s" %% "http4s-dsl" % "0.23.30",
      "org.http4s" %% "http4s-ember-client" % "0.23.30",
      "org.http4s" %% "http4s-ember-server" % "0.23.30",
      "io.circe" %% "circe-core" % "0.14.13",
      "io.circe" %% "circe-generic" % "0.14.13",
      "io.circe" %% "circe-parser" % "0.14.13",
      "org.typelevel" %% "cats-effect" % "3.6.1",
      "ch.qos.logback" % "logback-classic" % "1.5.18",
      "com.typesafe" % "config" % "1.4.3",
      "com.github.pureconfig" %% "pureconfig" % "0.17.9",
      "com.github.pureconfig" %% "pureconfig-generic" % "0.17.9",
      "org.postgresql" % "postgresql" % "42.7.5",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("module-info.class") => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    assembly / mainClass := Some("main.Main")
  )