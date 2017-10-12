import Dependencies._

/**
 * Build settings for utils
 */

lazy val commonSettings = Seq(
  organization := "fi.sn127",
  scalaVersion := "2.12.3",
  scalacOptions ++= Seq(
	"-Xlint",
	"-deprecation",
	"-feature",
	"-unchecked",
	"-Xfatal-warnings"),
  wartremoverErrors ++= Warts.allBut(
    Wart.Throw //https://github.com/puffnfresh/wartremover/commit/869763999fcc1fd685c1a8038c974854457b608f
  )
)

lazy val utils = (project in file(".")).
  aggregate(testing).
  settings(
    isSnapshot := false,
    publish := {},
    publishLocal := {}
  )

lazy val testing = (project in file("testing")).
  settings(commonSettings: _*).
  settings(
    name := "utils-testing",
    version := "0.7.0-SNAPSHOT",
    isSnapshot := false,
    fork in run := true,
    libraryDependencies += betterFiles,
    libraryDependencies += scalaArm,
    libraryDependencies += scalatest
  )

