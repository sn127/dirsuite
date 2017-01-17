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
      tc.execsToString(" " * 6 + "exec") + "\n" +
      "   failed test vector (output) after successful executions is: \n" +
      "     reference: [" + reference.toString + "]\n" +
      "     output:    [" + output.toString + "]"
  }
}

@SuppressWarnings(Array("org.wartremover.warts.ToString"))
final case class TestCase(testname: Path, execs: Seq[Array[String]], testVectors: Seq[TestVector]){
  val name: String = testname.toString
  val testPath = testname

  def execsToString(prefix: String): String = {
    execs.zipWithIndex
      .map({ case (args, idx) =>
        prefix + " %d:".format(idx) + " [" + args.mkString("", ",", "") + "]"
      }).mkString("\n")
  }

  def makeExecFailMsg(prefix: String, idx: Int, realArgs: Array[String]) = {
    prefix + "\n" +
      "   name: " + name + "]\n" +
      "   with execution sequence:\n" +
      execsToString(" " * 6 + "exec") + "\n" +
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


  protected def tokenizer(line: String): Array[String] = {
    if (line.isEmpty) {
      // No args-case => ok
      Array[String]()
    } else {
      val rawArgs = line.split(";", -1)
      if (rawArgs.size < 2){
        // it was string without any ';'
        // TODO ERROR
      }
      val sgrAwar = rawArgs.reverse
      val last = sgrAwar.head
      if (last.nonEmpty) {
        // there is trailing stuff after last ';',
        // so it could be missed semi-colon
        // TODO ERROR
      }
      // drop "last", and return list in correct order
      sgrAwar.drop(1).reverse
    }
  }

  protected def parseExec(testname: Path): Seq[Array[String]] = {
    val exexPrefix = "exec:"
    // Scala-ARM: managed close
    managed(io.Source.fromFile(testname.toString)).map(source => {
      val execLines = source.getLines
        .filter(!_.startsWith("#"))
        .filter(_.startsWith(exexPrefix))
        .map(_.stripPrefix(exexPrefix))
        .map(str => str.trim)
      val execs = execLines.map(tokenizer)
      execs.toList
    }).opt match {
      case Some(execs) => execs
      case None => Seq[Array[String]]()
    }
  }

  protected def mapArgs(testname: Path, args: Array[String]): Array[String] = {
    args
  }

  protected def findReferences(testdir: Path, testname: Path): Seq[Path] = {
    val fu = FileUtils(testdir.getFileSystem)
    val basename = fu.getBasename(testname) match {
      case (name, ext) => name
    }

    fu.findFiles(testdir, Glob(basename + ".ref.*"))
  }

  protected def mapOutput(testdir: Path, testname: Path, reference: Path): Path = {
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

  protected def registerDirSuiteTest(
    pattern: FindFilesPattern,
    tc: TestCase,
    testFuns: List[(Array[String]) => Any]) = {

    registerTest(pattern.toString + " => " + tc.name.toString) {
      testExecutor(tc, testFuns)
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
  def ignoreMultiTestDirSuite(basedir: Path, testPattern: FindFilesPattern)(
    beginTestFun: (Array[String] => Any),
    lastTestFun: (Array[String] => Any)) = {

    val fu = FileUtils(basedir.getFileSystem)
    val testnames = fu.findFiles(basedir, testPattern)

    testnames.foreach(test => registerIgnoredDirSuiteTest(testPattern, test))
  }

  def getTestcases(basedir: Path, testPattern: FindFilesPattern): Seq[TestCase] = {
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

    if (testnames.isEmpty) {
      throw new DirSuiteException("=>\n" +
        " " * 3 + "DirSuite is empty - there are no exec-files!\n" +
        " " * 6 + "basedir: [" + basedir.toString + "]\n" +
        " " * 6 + "pattern: " + testPattern.toString + "\n" +
        " " * 3 + "if this is intentional, you could ignore it"
        )
    }

    val testCases = testnames.map(testname => {
      val testdir = testname.getParent

      val execs = parseExec(testname)
      if (execs.isEmpty) {
        throw new DirSuiteException("=>\n" +
          " " * 3 + "Exec for test is empty - there is nothing to run!\n" +
          " " * 6 + "basedir: [" + basedir.toString + "]\n" +
          " " * 6 + "testname: [" + testname.toString + "]\n"
        )
      }

      val testVectors = findReferences(testdir, testname)
        .map(reference => {
          val output = mapOutput(testdir, testname, reference)
          val comparator = selectValidator(testname, reference, output)

          TestVector(reference, output, comparator)
        })

      TestCase(testname, execs, testVectors)
    })
    testCases
  }

  def runDirSuite(basedir: Path, testPattern: FindFilesPattern)(testFun: (Array[String] => Any)) = {

    getTestcases(basedir, testPattern).foreach(tc => {
      registerDirSuiteTest(testPattern, tc, List[(Array[String] => Any)](testFun))
    })
  }

  def runMultiTestDirSuite(basedir: Path, testPattern: FindFilesPattern)(
    beginTestFun: (Array[String] => Any),
    lastTestFun: (Array[String] => Any)) = {

    val testcases = getTestcases(basedir, testPattern)

    testcases.foreach(tc => {
      registerDirSuiteTest(testPattern, tc, List[(Array[String] => Any)](beginTestFun, lastTestFun))
    })
  }


  @SuppressWarnings(Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.TraversableOps"))
  protected def testExecutor(tc: TestCase, testFuns: List[(Array[String] => Any)]) = {

    if (tc.execs.length < testFuns.length) {
      throw new DirSuiteException("=>\n" +
        " " * 3 + "Exec line count is less than test function count. This is not supported!\n" +
        " " * 6 + "testname: " + tc.testname + "\n"
      )
    }
    /*
      Bake execs with testfuns

      val funs = List("a", "last")
      val is = List(1, 2, 3, 4, 5)
      is.reverse.zipAll(funs.reverse, 0, funs.head).reverse
      res1: List[(Int, String)] = List((1,a), (2,a), (3,a), (4,a), (5,last))
     */
    // TODO lengths
    val execsAndFunc: Seq[(Array[String], (Array[String]) => Any)] = tc.execs
      .reverse
      .zipAll(testFuns.reverse, Array[String](), testFuns.head)
      .reverse

    execsAndFunc.zipWithIndex.foreach({
      case ((args, testFun), index) =>

        val execArgs = mapArgs(tc.testname, args)
        try {

          /* this is real-deal for test run */
          testFun(execArgs)

        } catch {
          case tfe: TestFailedException =>
            throw tfe.modifyMessage(origMsg => {
              Option("" +
                tc.makeExecFailMsg(DirSuiteLike.executionFailureMsgPrefix, index, execArgs) +
                " " * 3 + "Failed result: \n" +
                " " * 6 + origMsg.getOrElse("") + "\n")
            })
          case ex: Exception =>
            throw new DirSuiteException("" +
              tc.makeExecFailMsg(DirSuiteLike.executionFailureMsgPrefix, index, execArgs) +
              " " * 3 + "Failed result: \n" +
              " " * 6 + ex.getMessage + "\n", ex)
        }
    })

    /*
     * validate test vectors
     */
    tc.testVectors.foreach({ testVector =>
      val compErrorMsg = try {

        /* this is real deal for test result validation */
        val compResult = testVector.validator(tc.testname, testVector.reference, testVector.output)

        compResult match {
          case None =>
            None
          case Some(cmpMsg) => Some(
            testVector.makeComparatorErrMsg(DirSuiteLike.testVectorFailureMsgPrefix, tc) + "\n" +
              " " * 3 + "Comparator: \n" +
              " " * 6 + "msg: " + cmpMsg + "\n"
          )
        }
      } catch {
        case ex: Exception => Some(
          testVector.makeComparatorErrMsg(DirSuiteLike.testVectorExceptionMsgPrefix, tc) + "\n" +
            " " * 3 + "Exception: \n" +
            " " * 6 + "cause: " + ex.getClass.getCanonicalName + "\n" +
            " " * 6 + "msg: " + ex.getMessage + "\n"
        )
      }
      // NOTE: Collect all comp results, and report all end results together (Cats ...)?
      if (compErrorMsg.nonEmpty) {
        throw new TestVectorException(compErrorMsg.getOrElse("Internal error in test framework (ex)!"))
      }
    })
  }
}