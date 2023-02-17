ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.8"

lazy val root = (project in file("."))
  .settings(
    name := "social-app-cesar-vigil"
  )

lazy val akkaVersion = "2.5.13"
val akkaHttpVersion = "10.1.7"
val scalaTestVersion = "3.0.5"
val logbackVersion = "1.2.10"
lazy val postgresVersion = "42.2.2"
lazy val json4sVersion = "3.2.11"

// some libs are available in Bintray's JCenter
resolvers += Resolver.jcenterRepo
//to make the in memory db works
resolvers += Resolver.bintrayRepo("dnvriend", "maven")

libraryDependencies ++= Seq(
  "com.typesafe.akka"          %% "akka-persistence" % akkaVersion,

  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,

  //Akka In Memory persistence
  "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.15.2",


  // akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,


  // JDBC with PostgreSQL
  "org.postgresql" % "postgresql" % postgresVersion,
  "com.github.dnvriend" %% "akka-persistence-jdbc" % "3.4.0",
)