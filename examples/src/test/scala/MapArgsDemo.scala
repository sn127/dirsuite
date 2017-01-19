import java.nio.file.{Path, Paths}

import fi.sn127.utils.fs.{FileUtils, Glob}
import fi.sn127.utils.testing.DirSuite

class MapArgsDemo extends DirSuite {
  /**
   * Get configuration path based on testname,
   * and add it and few other extra arguments to all
   * test executions calls.
   */
  override
  protected def mapArgs(testname: Path, args: Array[String]): Array[String] = {
    val fu = FileUtils(testname.getFileSystem)
    val basename = fu.getBasename(testname) match { case (base, ext) => base }
    val conf = basename.toString + ".conf"

    // For this demo, let's use only basename of our conf.
    // In real life, you probably want full path to conf-file,
    // and to have original args as last
    // e.g.
    // val testdir = fu.getParentDirPath(testname)
    // val fullPathConf = fu.getPath(testdir.toString, conf.toString).toString
    // Array("-c", fullPathConf) ++ Array("-X", "-q") ++ args

    args ++ Array("-c", conf) ++ Array("-X", "-q")
  }

  val testdir = Paths.get("tests").toAbsolutePath.normalize
  val app = new DemoApp(testdir)

  /**
   * Run all tests with our own version of args
   */
  runDirSuiteTestCases(testdir, Glob("args/txt[0-9]*.exec")) { args: Array[String] =>
    assertResult(DemoApp.SUCCESS) {
      app.doTxt(args)
    }
  }
}
