/*
 * Copyright 2016-2017 Jani Averbach <jaa@sn127.fi>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package fi.sn127.utils.testing

import java.nio.file.Path

import resource._

import fi.sn127.utils.fs.FileUtils

@SuppressWarnings(Array(
  "org.wartremover.warts.ToString",
  "org.wartremover.warts.NonUnitStatements"))
object TestRunner {

  val executionFailureMsgPrefix = "TEST FAILED WITH UNEXPECTED EXECUTION RESULT!"
  val testVectorFailureMsgPrefix = "TEST FAILED WITH UNEXPECTED TEST VECTOR RESULT!"
  val testVectorExceptionMsgPrefix = "TEST FAILED WITH EXCEPTION WHILE COMPARING TEST VECTORS!"

  def cmdsParser(cmdsPath: Path): List[Array[String]] = {
    // Scala-ARM: managed close
    managed(io.Source.fromFile(cmdsPath.toString)).map(source => {
      val cmdLines = source.getLines.map(str => str.trim).toList
      val cmdsAndArgs = cmdLines.map(cmd => cmd.split(";"))
      cmdsAndArgs
    }).opt match {
      case Some(cmdsAndArgs) => cmdsAndArgs
      case None => List[Array[String]]()
    }
  }

  def argsFeeder(testname: Path, args: Array[String]): Array[String] = {
    args
  }

  def findReferences(testdir: Path, testname: Path) : Seq[Path] = {
    val fu = FileUtils(testdir.getFileSystem)
    val basename = fu.getBasename(testname) match {case (name, ext) => name}

    fu.globFindFiles(testdir, basename + ".ref.*")
  }

  def getOutput(testdir: Path, testname: Path, reference: Path): Path = {
    val fu = FileUtils(testdir.getFileSystem)
    val basename = fu.getBasename(testname) match {case (name, ext) => name}

    val output = "out." + basename + "." + reference.getFileName.toString.stripPrefix(basename + ".ref.")
    fu.getPath(testdir.toString, output)
  }

  def selectComparator(testname: Path, reference: Path, output: Path): ((Path, Path) => Boolean) = {
    val fu = FileUtils(testname.getFileSystem)

    val refExt = fu.getBasename(reference)
    refExt match {
      case (_, ext) => ext match {
        case Some("txt") => TestComparator.txtComparator
        case Some("xml") => TestComparator.xmlComparator
        case _ => TestComparator.txtComparator
      }
    }
  }

  /**
   * This function should catch all exceptions which are potentially thrown by specimen
   *
   * @param basedir    name of dir which holds test dirs
   * @param dirPattern pattern of name of test dir names (regex)
   * @param specimen   returns false if execution failed
   */
  def run(basedir: Path, dirPattern: String, specimen: (Array[String] => Boolean)) {
    val fu = FileUtils(basedir.getFileSystem)

    val testnames = fu.regexFindFiles(basedir, dirPattern)

    val testCases = testnames.map(testname => {
      val testdir = testname.getParent

      val cmds = cmdsParser(testname)

      val testVectors = findReferences(testdir, testname)
        .map(reference => {
          val output = getOutput(testdir, testname, reference)
          val comparator = selectComparator(testname, reference, output)

          TestVector(reference, output, comparator)
        })

      TestCase(testname, cmds, testVectors)
    })

    for (tc <- testCases) {
      testExecutor(tc)
    }

    def testExecutor(tc: TestCase) = {
      def cmdsAndArgsStr(prefix: String): String = {
        tc.cmdsAndArgs.map((cmds: Array[String]) => {
          prefix + "[" + cmds.mkString("", ",", "") + "]"
        }).mkString("\n")
      }

      for (args <- tc.cmdsAndArgs) {
        def execFailMsg = {
          executionFailureMsgPrefix + "\n" +
            "   name: " + tc.name.toString + "]\n" +
            "   with execution sequence:\n" +
            cmdsAndArgsStr(" " * 6 + "exec: ") + "\n" +
            "   actual failed execution is: \n" +
            args.mkString(" " * 6 + "exec: [", ",", "]") + "\n"
        }

        val success = specimen(argsFeeder(tc.name, args))
        val failureMsg = if (success) {
          None
        } else {
          Some(execFailMsg)
        }
        assert(failureMsg.isEmpty, failureMsg.getOrElse("Internal error in test framework (exec)!")
        )
      }
      for (testVector <- tc.testVectors) {
        def makeCompErrMsg(prefix: String) = {
          prefix + "\n" +
            "   with name: " + tc.name.toString + "]\n" +
            "   with execution sequence:\n" +
            cmdsAndArgsStr(" " * 6 + "exec: ") + "\n" +
            "   failed test vector (output) after successful executions is: \n" +
            "     reference: [" + testVector.reference.toString + "]\n" +
            "     output:    [" + testVector.output.toString + "]\n"
        }

        val compErrorMsg = try {
          val same = testVector.comparator(testVector.output, testVector.reference)
          if (same) {
            None
          } else {
            Some(makeCompErrMsg(testVectorFailureMsgPrefix))
          }
        } catch {
          case ex: Exception =>
            Some(
              makeCompErrMsg(testVectorExceptionMsgPrefix) + "\n" +
                "Exception: \n" +
                "   message: " + ex.getMessage + "\n"
            )
        }
        // NOTE: Collect all comp results, and report all end results together (Cats ...)?
        assert(compErrorMsg.isEmpty, compErrorMsg.getOrElse("Internal error in test framework (ex)!"))
      }
    }
  }
}
