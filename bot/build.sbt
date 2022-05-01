ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.2"
val AkkaVersion = "2.6.19"
val AkkaHttpVersion = "10.2.9"
val doobieVersion = "1.0.0-RC1"

lazy val root = (project in file("."))
  .settings(
    name := "trading-bot",
    idePackagePrefix := Some("ch.xavier"),
    libraryDependencies ++= Seq(
      ("com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion).cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-stream" % AkkaVersion).cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-http" % AkkaHttpVersion).cross(CrossVersion.for3Use2_13),
      "io.spray" %%  "spray-json" % "1.3.6",
      "org.tpolecat" %% "doobie-core"      % doobieVersion,
      "org.tpolecat" %% "doobie-postgres"  % doobieVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.11",
      "org.ta4j" % "ta4j-core" % "0.14"
    )
  )
