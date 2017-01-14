import sbt._

object Dependencies {

  /*
   * Versions
   */
  val scalaArmVersion = "2.0"
  val scalatestVersion = "3.0.1"

  /*
   * Libraries: scala
   */
  val scalaArm = "com.jsuereth" %% "scala-arm" % scalaArmVersion
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion

  /*
   * Libraries: java
   */
}
