package fi.sn127.utils.testing

import java.nio.file.{Files, Path}

import fi.sn127.utils.fs.FileUtils

@SuppressWarnings(Array(
  "org.wartremover.warts.ToString",
  "org.wartremover.warts.NonUnitStatements"))
object TestRunner  {

  /**
    * This function should catch all exceptions which are potentially thrown by specimen
    *
    * @param basedir    name of dir which holds test dirs
    * @param dirPattern pattern of name of test dir names (regex)
    * @param specimen   returns false if execution failed
    */
  def run(basedir: Path, dirPattern: String, specimen: (Array[String] => Boolean)) {
    val fu = FileUtils(basedir.getFileSystem)
    val testConfs = fu.regexFindFiles(basedir, dirPattern)

    val testCases = for (f <- testConfs) yield {
      val testDir = f.getParent
      val basename = f.getFileName.toString.stripSuffix(".conf")

      val argsPath = fu.getPath(testDir.toString, basename + ".cmds")
      val args =
        if (Files.isReadable(argsPath)) {
          val source = io.Source.fromFile(argsPath.toString)
          try source.getLines.map(str => str.trim).toList.map(cmd => cmd.split(";")) finally source.close
        } else {
          List[Array[String]]()
        }

      val refFiles = fu.globFindFiles(testDir,  basename + ".ref.*")

      val testVecs = for (rf <- refFiles) yield {
        val ref = rf.getFileName.toString
        val output = "out." + basename + "." + ref.stripPrefix(basename + ".ref.")

        /*
        if (ref.endsWith(".xml")) {
          TestVec(testDir.toString + "/" + output,
            testDir.toString + "/" + ref, TestComparator.xmlComparator)
        } else {
        }
        */
        TestVec(testDir.toString + "/" + output,
          testDir.toString + "/" + ref, TestComparator.txtComparator)
      }
      TestCase(f.toString, args, testVecs.toList)
    }

    for (tc <- testCases) {
      for (cmd <- tc.args) {
        val success = specimen(cmd)
        if (!success) {
          println("UNEXPECTED EXEC RESULT!")
          println("   with conf: " + tc.conf.toString + "]")
          print("   with args: ")
          cmd.foreach(x => print("\"" + x + "\" "))
          println("")
          assert(false)
        }
      }
      for (testfiles <- tc.testVec) {
        val sameResult = testfiles.comparator(testfiles.output, testfiles.reference)
        if (!sameResult) {
          println("TEST FAILED: " + tc.conf.toString)
          println(" Failed ref: " + testfiles.reference)
          println(" Failed out: " + testfiles.output)
        }
        assert(sameResult)
      }
    }
  }
}
