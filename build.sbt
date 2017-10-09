
name := "tickets"

version := "0.1"

scalaVersion := "2.12.3"

libraryDependencies ++=
  Seq(
    "com.typesafe.akka" %% "akka-http" % "10.0.10",
    "de.heikoseeberger" %% "akka-http-circe" % "1.18.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.scalatest" %% "scalatest" % "3.0.1" % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % "10.0.10" % Test
  ) ++
  Seq("io.circe" %% "circe-core", "io.circe" %% "circe-generic", "io.circe" %% "circe-parser").map(_ % "0.8.0")