package fi.sn127.utils.testing

import java.nio.file.{Files, Paths}

import fi.sn127.utils.fs.FileUtils

object TestRunner  {

  /**
    * This function should catch all exceptions which are potentially thrown by specimen
    *
    * @param basedir name of dir which holds test dirs
    * @param dirPattern pattern of name of test dir names (regex)
    * @param pecimen returns false if execution failed
    */
  def run(basedir: String, dirPattern: String, specimen: (Array[String] => Boolean) ) {

    val testConfs = for (d <- FileUtils.listDirs(basedir, dirPattern)) yield {
        for (f <- FileUtils.listFiles(d.getAbsolutePath, ".*\\.conf")) yield { f }
      }

    val testCases = for (f <- testConfs.flatten) yield {
      val testDir = f.toPath.getParent
      val basename = f.toPath.getFileName.toString.stripSuffix(".conf")

      val argsPath = Paths.get(testDir.toString, basename + ".cmds")
      val args =
        if (Files.isReadable(argsPath)) {
          val source = io.Source.fromFile(argsPath.toString)
          try source.getLines.map(str => str.trim).toList.map(cmd => cmd.split(";")) finally source.close
        } else {
          List[Array[String]]()
        }

      val refFiles = FileUtils.listFiles(testDir.toString, ".*/" + basename + "\\.ref\\..*")

      val testVecs = for (rf <- refFiles) yield {
        val ref = rf.toPath.getFileName.toString
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
      TestCase(f.toPath.toString, args, testVecs.toList)
    }

    for (tc <- testCases) {
        for(cmd <- tc.args) {
          val success = specimen(cmd)
          if (!success) {
            println("EXEC FAILS: [" + tc.conf.toString + "]")
            print  ("      ARGS: [")
            cmd.foreach(x => print("[" + x + "],"))
            println("]")
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
