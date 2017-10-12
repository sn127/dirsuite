import sbt._

object Dependencies {

  /*
   * Versions
   */
  val betterFilesVersion = "3.1.0"
  val scalaArmVersion = "2.0"
  val scalatestVersion = "3.0.4"

  /*
   * Libraries: scala
   */
  val betterFiles = "com.github.pathikrit" %% "better-files" % betterFilesVersion
  val scalaArm = "com.jsuereth" %% "scala-arm" % scalaArmVersion
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion

  /*
   * Libraries: java
   */
}
