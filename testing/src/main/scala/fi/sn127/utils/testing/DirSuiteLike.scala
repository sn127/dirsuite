/*
 * Copyright 2016-2017 Jani Averbach
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

import scala.util.{Failure, Success}

import fi.sn127.utils.fs.{FileUtils, FindFilesPattern, Glob}

@SuppressWarnings(Array(
"org.wartremover.warts.Null",
"org.wartremover.warts.DefaultArguments"))
class DirSuiteException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause) {}

@SuppressWarnings(Array(
  "org.wartremover.warts.Null",
  "org.wartremover.warts.DefaultArguments"))
class TestVectorException(msg: String, cause: Throwable = null) extends DirSuiteException(msg, cause)


final case class TestVector(reference: Path, output: Path, validator: (Path, Path, Path) => Option[String]) {

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  def makeComparatorErrMsg(prefix: String, tc: TestCase) = {
    prefix + "\n" +
      "   with name: [" + tc.name + "]\n" +
      "   with execution sequence:\n" +
      tc.execsStr(" " * 6 + "exec") + "\n" +
      "   failed test vector (output) after successful executions is: \n" +
      "     reference: [" + reference.toString + "]\n" +
      "     output:    [" + output.toString + "]"
  }
}

@SuppressWarnings(Array("org.wartremover.warts.ToString"))
final case class TestCase(testname: Path, execs: Seq[Array[String]], testVectors: Seq[TestVector]){
  val name: String = testname.toString
  val testPath = testname

  def execsStr(prefix: String): String = {
    execs.zipWithIndex
      .map({ case (args, idx) =>
        prefix + " %d:".format(idx) + " [" + args.mkString("", ",", "") + "]"
      }).mkString("\n")
  }

  def execFailMsg(prefix: String, idx: Int, realArgs: Array[String]) = {
    prefix + "\n" +
      "   name: " + name + "]\n" +
      "   with execution sequence:\n" +
      execsStr(" " * 6 + "exec") + "\n" +
      "   actual failed execution is: \n" +
      realArgs.mkString(" " * 6 + "exec " + "%d:".format(idx) + " [", ",", "]") + "\n"
  }
}

object DirSuiteLike {
  val executionFailureMsgPrefix = "TEST FAILED WITH UNEXPECTED EXECUTION RESULT!"
  val testVectorFailureMsgPrefix = "TEST FAILED WITH UNEXPECTED TEST VECTOR RESULT!"
  val testVectorExceptionMsgPrefix = "TEST FAILED WITH EXCEPTION WHILE COMPARING TEST VECTORS!"
}

@SuppressWarnings(Array(
  "org.wartremover.warts.ToString",
  "org.wartremover.warts.NonUnitStatements"))
trait DirSuiteLike extends FunSuiteLike {


  protected def execParser(testname: Path): Seq[Array[String]] = {
    // Scala-ARM: managed close
    managed(io.Source.fromFile(testname.toString)).map(source => {
      val execLines = source.getLines.map(str => str.trim).toList
      val execs = execLines.map(cmd => cmd.split(";"))
      execs
    }).opt match {
      case Some(execs) => execs
      case None => Seq[Array[String]]()
    }
  }

  protected def argsMapping(testname: Path, args: Array[String]): Array[String] = {
    args
  }

  protected def findReferences(testdir: Path, testname: Path): Seq[Path] = {
    val fu = FileUtils(testdir.getFileSystem)
    val basename = fu.getBasename(testname) match {
      case (name, ext) => name
    }

    fu.findFiles(testdir, Glob(basename + ".ref.*"))
  }

  protected def getOutput(testdir: Path, testname: Path, reference: Path): Path = {
    val fu = FileUtils(testdir.getFileSystem)
    val basename = fu.getBasename(testname) match {
      case (name, ext) => name
    }

    val output = "out." + basename + "." + reference.getFileName.toString.stripPrefix(basename + ".ref.")
    fu.getPath(testdir.toString, output)
  }

  protected def selectValidator(testname: Path, reference: Path, output: Path): ((Path, Path, Path) => Option[String]) = {
    val fu = FileUtils(testname.getFileSystem)

    val refExt = fu.getBasename(reference)
    refExt match {
      case (_, ext) => ext match {
        case Some("txt") => TestValidator.txtValidator
        case Some("xml") => TestValidator.xmlValidator
        case _ => TestValidator.txtValidator
      }
    }
  }

  protected def registerDirSuiteTest(pattern: FindFilesPattern, tc: TestCase, testFun: (Array[String]) => Any) = {
    registerTest(pattern.toString + " => " + tc.name.toString) {
      testExecutor(tc, testFun)
    }
  }

  protected def registerIgnoredDirSuiteTest(pattern: FindFilesPattern, testname: Path) = {
    registerIgnoredTest(pattern.toString + " => " + testname.toString) {}
  }

  def ignoreDirSuite(basedir: Path, testPattern: FindFilesPattern)(testFun: (Array[String] => Any)) = {
    val fu = FileUtils(basedir.getFileSystem)
    val testnames = fu.findFiles(basedir, testPattern)

    testnames.foreach(test => registerIgnoredDirSuiteTest(testPattern, test))
  }

  /**
   * This function should catch all exceptions which are potentially thrown by specimen
   *
   * @param basedir     name of dir which holds test dirs
   * @param testPattern pattern of name of test names
   * @param testFun    returns false if execution failed
   */
  def runDirSuite(basedir: Path, testPattern: FindFilesPattern)(testFun: (Array[String] => Any)) = {
    val fu = FileUtils(basedir.getFileSystem)

    fu.ensurePath(basedir) match {
      case Success(ok) =>
      case Failure(ex) => throw new DirSuiteException("=>\n" +
        " " * 3 + "The basedir for DirSuite is invalid\n" +
        " " * 6 + "basedir: [" + basedir.toString + "]\n" +
        " " * 6 + "Exception: " + ex.getClass.getCanonicalName + "\n" +
        " " * 9 + "Msg: " + ex.getMessage + "\n"
      )
    }

    val testnames = fu.findFiles(basedir, testPattern)

    val testCases = testnames.map(testname => {
      val testdir = testname.getParent

      val execs = execParser(testname)

      val testVectors = findReferences(testdir, testname)
        .map(reference => {
          val output = getOutput(testdir, testname, reference)
          val comparator = selectValidator(testname, reference, output)

          TestVector(reference, output, comparator)
        })

      TestCase(testname, execs, testVectors)
    })

    for (tc <- testCases) {
      registerDirSuiteTest(testPattern, tc, testFun)
    }
  }

  @SuppressWarnings(Array(
    "org.wartremover.warts.Any"))
  protected def testExecutor(tc: TestCase, testFun: (Array[String] => Any)) = {

    tc.execs.zipWithIndex.foreach({
      case (args, index) =>
        val execArgs = argsMapping(tc.testname, args)
        try {

          /* this is real-deal for test run */
          val v = testFun(execArgs)

        } catch {
          case tfe: TestFailedException =>
            throw tfe.modifyMessage(origMsg => {
              Option("" +
                tc.execFailMsg(DirSuiteLike.executionFailureMsgPrefix, index, execArgs) +
                " " * 3 + "Failed result: \n" +
                " " * 6 + origMsg.getOrElse("") + "\n")
            })
        }
        for (testVector <- tc.testVectors) {
          val compErrorMsg = try {

            /* this is real deal for test result validation */
            val compResult = testVector.validator(tc.testname, testVector.reference, testVector.output)

            compResult match {
              case None =>
                None
              case Some(cmpMsg) => Some(
                testVector.makeComparatorErrMsg(DirSuiteLike.testVectorFailureMsgPrefix, tc) + "\n" +
                  "Comparator: \n" +
                  "   msg: " + cmpMsg + "\n"
              )
            }
          } catch {
            case ex: Exception => Some(
                testVector.makeComparatorErrMsg(DirSuiteLike.testVectorExceptionMsgPrefix, tc) + "\n" +
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