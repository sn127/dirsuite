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

import org.scalatest.FunSuiteLike
import org.scalatest.exceptions.TestFailedException
import resource._

import fi.sn127.utils.fs.{FileUtils, FindFilesPattern, Glob}

@SuppressWarnings(Array(
  "org.wartremover.warts.Null",
  "org.wartremover.warts.DefaultArguments"))
class TestVectorException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause) {}

final case class TestVector(reference: Path, output: Path, comparator: (Path, Path) => Option[String])

@SuppressWarnings(Array("org.wartremover.warts.ToString"))
final case class TestCase(cmds: Path, cmdsAndArgs: Seq[Array[String]], testVectors: Seq[TestVector]){
  val name: String = cmds.toString
}


@SuppressWarnings(Array(
  "org.wartremover.warts.ToString",
  "org.wartremover.warts.NonUnitStatements"))
trait TestRunnerLike extends FunSuiteLike {

  val executionFailureMsgPrefix = "TEST FAILED WITH UNEXPECTED EXECUTION RESULT!"
  val testVectorFailureMsgPrefix = "TEST FAILED WITH UNEXPECTED TEST VECTOR RESULT!"
  val testVectorExceptionMsgPrefix = "TEST FAILED WITH EXCEPTION WHILE COMPARING TEST VECTORS!"

  def cmdsParser(cmdsPath: Path): Seq[Array[String]] = {
    // Scala-ARM: managed close
    managed(io.Source.fromFile(cmdsPath.toString)).map(source => {
      val cmdLines = source.getLines.map(str => str.trim).toList
      val cmdsAndArgs = cmdLines.map(cmd => cmd.split(";"))
      cmdsAndArgs
    }).opt match {
      case Some(cmdsAndArgs) => cmdsAndArgs
      case None => Seq[Array[String]]()
    }
  }

  def argsFeeder(testname: Path, args: Array[String]): Array[String] = {
    args
  }

  def findReferences(testdir: Path, testname: Path): Seq[Path] = {
    val fu = FileUtils(testdir.getFileSystem)
    val basename = fu.getBasename(testname) match {
      case (name, ext) => name
    }

    fu.findFiles(testdir, Glob(basename + ".ref.*"))
  }

  def getOutput(testdir: Path, testname: Path, reference: Path): Path = {
    val fu = FileUtils(testdir.getFileSystem)
    val basename = fu.getBasename(testname) match {
      case (name, ext) => name
    }

    val output = "out." + basename + "." + reference.getFileName.toString.stripPrefix(basename + ".ref.")
    fu.getPath(testdir.toString, output)
  }

  def selectComparator(testname: Path, reference: Path, output: Path): ((Path, Path) => Option[String]) = {
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

  def registerDirSuiteTest(pattern: FindFilesPattern, tc: TestCase, specimen: (Array[String]) => Any) = {
    // TODO: This function is not tested automatically at all!
    // because it is overloaded while testing tester.
    // Figure out how to test nested FunSuite while still
    // running this register intact
    registerTest(pattern.toString + " => " + tc.name.toString) {
      testExecutor(tc, specimen)
    }
  }

  def registerIgnoredDirSuiteTest(dirPattern: String, cmdsPath: Path) = {
    registerIgnoredTest(dirPattern + " => " + cmdsPath.toString) {}
  }

  def ignoreDirSuite(basedir: Path, dirPattern: FindFilesPattern)(specimen: (Array[String] => Any)) = {
    val fu = FileUtils(basedir.getFileSystem)
    val testnames = fu.findFiles(basedir, dirPattern)

    testnames.foreach(test => registerIgnoredDirSuiteTest(dirPattern.toString, test))
  }

  /**
   * This function should catch all exceptions which are potentially thrown by specimen
   *
   * @param basedir     name of dir which holds test dirs
   * @param testPattern pattern of name of test names
   * @param specimen    returns false if execution failed
   */
  def runDirSuite(basedir: Path, testPattern: FindFilesPattern)(specimen: (Array[String] => Any)) = {
    val fu = FileUtils(basedir.getFileSystem)

    val testnames = fu.findFiles(basedir, testPattern)

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
      registerDirSuiteTest(testPattern, tc, specimen)
    }
  }

  @SuppressWarnings(Array(
    "org.wartremover.warts.Any"))
  def testExecutor(tc: TestCase, specimen: (Array[String] => Any)) = {

    def cmdsAndArgsStr(prefix: String): String = {
      tc.cmdsAndArgs.zipWithIndex
        .map({ case (args, idx) =>
          prefix + " %d:".format(idx) + " [" + args.mkString("", ",", "") + "]"
        }).mkString("\n")
    }

    tc.cmdsAndArgs.zipWithIndex.foreach({
      case (args, index) =>
        def execFailMsg(idx: Int) = {
          executionFailureMsgPrefix + "\n" +
            "   name: " + tc.name.toString + "]\n" +
            "   with execution sequence:\n" +
            cmdsAndArgsStr(" " * 6 + "exec") + "\n" +
            "   actual failed execution is: \n" +
            args.mkString(" " * 6 + "exec " + "%d:".format(idx) + " [", ",", "]") + "\n"
        }

        try {
          val v = specimen(argsFeeder(tc.cmds, args))
        } catch {
          case tfe: TestFailedException =>
            throw tfe.modifyMessage(origMsg => {
              Option("" +
                execFailMsg(index) +
                " " * 3 + "Failed result: \n" +
                " " * 6 + origMsg.getOrElse("") + "\n")
            })
        }
        for (testVector <- tc.testVectors) {
          def makeComparatorErrMsg(prefix: String) = {
            prefix + "\n" +
              "   with name: [" + tc.name.toString + "]\n" +
              "   with execution sequence:\n" +
              cmdsAndArgsStr(" " * 6 + "exec") + "\n" +
              "   failed test vector (output) after successful executions is: \n" +
              "     reference: [" + testVector.reference.toString + "]\n" +
              "     output:    [" + testVector.output.toString + "]"
          }
          val compErrorMsg = try {
            testVector.comparator(testVector.output, testVector.reference) match {
              case None =>
                None
              case Some(cmpMsg) => Some(
                makeComparatorErrMsg(testVectorFailureMsgPrefix) + "\n" +
                  "Comparator: \n" +
                  "   msg: " + cmpMsg + "\n"
              )
            }
          } catch {
            case ex: Exception => Some(
                makeComparatorErrMsg(testVectorExceptionMsgPrefix) + "\n" +
                  "Exception: \n" +
                  "   cause: " + ex.getClass.getCanonicalName + "\n" +
                  "   msg: " + ex.getMessage + "\n"
              )
          }
          // NOTE: Collect all comp results, and report all end results together (Cats ...)?
          if (compErrorMsg.nonEmpty) {
            throw new TestVectorException(compErrorMsg.getOrElse("Internal error in test framework (ex)!"))
          }
        }
    })
  }
}