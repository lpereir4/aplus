name := """aplus"""
organization := "fr.gouv.beta"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

//libraryDependencies += filters

libraryDependencies ++= Seq(
  ws,
  jdbc,
  evolutions
)

pipelineStages := Seq(rjs, digest, gzip)

libraryDependencies += specs2 % Test
libraryDependencies += guice

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.2.2",
  "org.playframework.anorm" %% "anorm" % "2.6.2",
  "com.typesafe.play" %% "play-mailer" % "7.0.0",
  "com.sun.mail" % "javax.mail" % "1.6.1",
  "com.typesafe.play" %% "play-mailer-guice" % "7.0.0",
  "com.typesafe.play" %% "play-json-joda" % "2.6.13",
  "net.jcazevedo" %% "moultingyaml" % "0.4.0",
  "com.google.guava" % "guava" % "25.1-jre"
)
// UI
libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.6.3",
  "org.webjars.bower" % "material-design-lite" % "1.3.0",
  "org.webjars" % "material-design-icons" % "3.0.1",
  "org.webjars.npm" % "roboto-fontface" % "0.9.0",
  "org.webjars.npm" % "dialog-polyfill" % "0.4.9",
  "org.webjars.npm" % "twemoji" % "2.5.1",
  "org.webjars" % "chartjs" % "2.7.2"
)
// Crash
libraryDependencies += "io.sentry" % "sentry-logback" % "1.7.5"

// Adds additional packages into Twirl

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "fr.gouv.beta.binders._"

