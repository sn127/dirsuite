
import java.nio.file.Paths

import fi.sn127.utils.fs.{Glob, Regex}
import fi.sn127.utils.testing.DirSuite

class DirSuiteDemo extends DirSuite {

  val testdir = Paths.get("tests").toAbsolutePath.normalize
  val app = new DemoApp(testdir)

  /**
   * Find all noargs-tests and execute them.
   *
   * Assert that App (doArgsCount) return correct arg count (0)
   *
   * Search method: Glob
   * https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob
   */
  runDirSuite(testdir, Glob("success/noargs[0-9]*.exec")) { args: Array[String] =>
    assertResult(0) {
      app.doArgsCount(args)
    }
  }

  /**
   * Find all args3 and execute them.
   *
   * Assert that App (doArgsCount) return correct arg count (3)
   *
   * Search method: Glob
   * https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob
   */
  runDirSuite(testdir, Glob("success/args3-[0-9]*.exec")) { args: Array[String] =>
    assertResult(3) {
      app.doArgsCount(args)
    }
  }

  /**
   * Find all txt-output tests and execute them.
   *
   * Assert that doTxt was successful and check
   * output based on reference vectors.
   *
   * Search method: Regex
   * https://docs.oracle.com/javase/tutorial/essential/regex/index.html
   */
  runDirSuite(testdir, Regex("success/txt[0-9]+\\.exec")) { args: Array[String] =>
    assertResult(DemoApp.SUCCESS) {
      app.doTxt(args)
    }
  }

  /**
   * Find all xml-output tests and execute them.
   *
   * Assert that doXml was successful and check
   * output based on reference vectors.
   *
   * Search method: Regex
   * https://docs.oracle.com/javase/tutorial/essential/regex/index.html
   */
  runDirSuite(testdir, Regex("success/xml[0-9]+\\.exec")) { args: Array[String] =>
    assertResult(DemoApp.SUCCESS) {
      app.doXml(args)
    }
  }

  /**
   * Find all mixed output tests and execute them.
   * Use XML Validator for XML-files, and TXT Validator
   * for txt-files. This choice is done by
   * DirSuite::selectValidator, which can be overloaded
   * test-by-test class basis.
   *
   * Assert that doTxtXml was successful and check
   * output based on reference vectors.
   *
   * Search method: Glob
   * https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob
   */
  runDirSuite(testdir, Glob("success/mixed[0-9]*.exec")) { args: Array[String] =>
    assertResult(DemoApp.SUCCESS) {
      app.doTxtXml(args)
    }
  }

  /**
   * Test that an exception is thrown.
   *
   * This is internal assertThrows/intercept, so every execution step
   * must throw an exception if multiple steps are run.
   */
  runDirSuite(testdir, Glob("success/singleStepEx[0-9]*.exec")) { args: Array[String] =>
    assertThrows[RuntimeException]{
      app.doFlaky(args)
    }
  }

  /**
   * First execution steps must succeed, and then
   * Last execution step  must throw up an exception
   * when multiple steps are run
   *
   * For example:
   *  exec 0 => assertResult(SUCCESS)
   *  exec 1 => assertResult(SUCCESS)
   *  exec 2 => assertThrows[RuntimeException]
   */
  runMultiTestDirSuite(testdir, Glob("success/multiStepEx[0-9]*.exec"))(
    { args: Array[String] =>
      /*
       * All steps at the begin must succeed
       */
      assertResult(DemoApp.SUCCESS) {
        app.doFlaky(args)
      }
    },
    { args: Array[String] =>
      /*
       * Last step must fail with exception
       */
      assertThrows[RuntimeException] {
        app.doFlaky(args)
      }
    }
  )
  /**
   * First execution steps must succeed, and then
   * Last execution step  must fail
   *
   * For example:
   *  exec 0 => assertResult(SUCCESS)
   *  exec 1 => assertResult(SUCCESS)
   *  exec 2 => assertResult(FAILURE)
   */
  runMultiTestDirSuite(testdir, Glob("success/multiStepFail[0-9]*.exec"))(
    { args: Array[String] =>
      /*
       * All steps at the begin must succeed
       */
      assertResult(DemoApp.SUCCESS) {
        app.doFlaky(args)
      }
    },
    { args: Array[String] =>
      /*
       * Last step must fail
       */
      assertResult(DemoApp.FAILURE) {
        app.doFlaky(args)
      }
    }
  )
}
