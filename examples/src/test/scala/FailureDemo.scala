import java.nio.file.Paths

import fi.sn127.utils.testing.{Glob, Regex}
import fi.sn127.utils.testing.DirSuite

class FailureDemo extends DirSuite {

  val testdir = Paths.get("tests").toAbsolutePath.normalize
  val app = new DemoApp(testdir)
  /**
   * Example how to ignore tests which are
   * failing at the moment.
   *
   * To see actual errors, change these to
   * ignoreDirSuiteTestCases => runDirSuiteTestCases
   */


  /**
   * Exec failure: Normal assertion error
   */
  ignoreDirSuiteTestCases(testdir, Glob("success/args3-[0-9]*.exec")) { args: Array[String] =>
    assertResult(2) {
      app.doArgsCount(args)
    }
  }

  /**
   * Exec failure: Exception
   * TODO: testing, this needs better error message
   */
  ignoreDirSuiteTestCases(testdir, Glob("success/singleStepEx[0-9]*.exec")) { args: Array[String] =>
    assertResult(DemoApp.SUCCESS) {
      app.doFlaky(args)
    }
  }

  /**
   * Test Vector failure: missing output
   */
  ignoreDirSuiteTestCases(testdir, Glob("failure/fileNotFound[0-9]*.exec")) { args: Array[String] =>
    assertResult(DemoApp.SUCCESS) {
      app.doTxt(args)
    }
  }

  /**
   * Test Vector failure: Data differs (reference != output)
   */
  ignoreDirSuiteTestCases(testdir, Glob("failure/content[0-9]*.exec")) { args: Array[String] =>
    assertResult(DemoApp.SUCCESS) {
      app.doTxt(args)
    }
  }

  /**
   * Test Vector failure: Exception while validating vector
   */
  ignoreDirSuiteTestCases(testdir, Glob("failure/xmlEx[0-9]*.exec")) { args: Array[String] =>
    assertResult(DemoApp.SUCCESS) {
      app.doTxt(args)
    }
  }
}
