ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / maintainer := "rumeaug17@gmail.com"

ThisBuild / scalaVersion := "3.1.3"

val AkkaVersion = "2.6.19"

ThisBuild / libraryDependencies ++= Seq(
  "com.lihaoyi" %% "upickle" % "2.0.0",
  "com.lihaoyi" %% "requests" % "0.7.0",
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  //"com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "org.rg" %% "scala-util3" % "1.0.0"
)

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

lazy val root = (project in file("."))
  .settings(
    name := "scala-blockchain",
    organization := "org.rg",
    idePackagePrefix := Some("org.rg.sbc")
)
