package fi.sn127.utils.testing

import java.nio.file.{FileSystems, Path}

import org.scalatest.{FunSpecLike, Inside, Matchers}

import fi.sn127.utils.fs.{FileUtils, FindFilesPattern, Glob, Regex}

object DummyApp {
  val SUCCESS = 1
  val FAILURE = 0

  def mainSuccess(args: Array[String]): Int = {
    SUCCESS
  }

  def mainFail(args: Array[String]): Int = {
    FAILURE
  }
}

@SuppressWarnings(Array(
  "org.wartremover.warts.ToString"))
class TestRunnerDemo extends TestRunnerLike with FunSpecLike with Matchers with Inside {

  override
  def testExecutorRegister(pattern: FindFilesPattern, tc: TestCase, specimen: (Array[String]) => Any) = {
    registerTest(pattern.toString + " => " + tc.name.toString) {
      testExecutor(tc, specimen)
    }
  }

  override
  def testIgnoreRegister(dirPattern: String, cmdsPath: Path) = {
    registerIgnoredTest(dirPattern + " => " + cmdsPath.toString) {}
  }


  val filesystem = FileSystems.getDefault
  val testdir = filesystem.getPath("tests/testrunner").toAbsolutePath.normalize
  val fu = FileUtils(filesystem)


  runDirSuite(testdir, Regex("success/txt[0-9]+\\.cmds")) { args: Array[String] =>
    assertResult(DummyApp.SUCCESS) {
      DummyApp.mainSuccess(args)
    }
  }

  ignoreDirSuite(testdir, Glob("success/tr*.cmds")) { args: Array[String] =>
    assertResult(DummyApp.SUCCESS) {
      DummyApp.mainFail(args)
    }
  }
}