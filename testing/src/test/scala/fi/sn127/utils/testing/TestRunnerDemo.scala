package fi.sn127.utils.testing

import java.nio.file.FileSystems

import fi.sn127.utils.fs.{Glob, Regex}

object DemoProg {
  val SUCCESS = 1
  val FAILURE = 0

  def mainSuccess(args: Array[String]): Int = {
    SUCCESS
  }

  def mainFailure(args: Array[String]): Int = {
    FAILURE
  }
}

@SuppressWarnings(Array(
  "org.wartremover.warts.ToString"))
class DemoDirSuiteTest extends TestRunnerLike {

  val filesystem = FileSystems.getDefault
  val testdir = filesystem.getPath("tests/testrunner").toAbsolutePath.normalize

  runDirSuite(testdir, Regex("success/txt[0-9]+\\.cmds")) { args: Array[String] =>
    assertResult(DemoProg.SUCCESS) {
      DemoProg.mainSuccess(args)
    }
  }

  ignoreDirSuite(testdir, Glob("success/tr*.cmds")) { args: Array[String] =>
    assertResult(DemoProg.SUCCESS) {
      DemoProg.mainFailure(args)
    }
  }
}
