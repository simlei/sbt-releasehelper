// For information on how to use this plugin, see the accompanying Readme.md document.
scalaVersion := "2.10.6"
version  := "0.43-SNAPSHOT"
organization := "de.opal-project"
licenses += ("BSC 2-clause", url("https://opensource.org/licenses/BSD-2-Clause"))

sbtPlugin := true

publishMavenStyle := false

name := "sbt-releasehelper"
description := "Provides automations for creating new releases of OPAL"

resolvers += "novus repo" at "http://repo.novus.com/releases/"

libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.0.3" % "test")
libraryDependencies += "commons-net" % "commons-net" % "3.6"
libraryDependencies += "com.typesafe" % "config" % "1.3.1"

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.6")