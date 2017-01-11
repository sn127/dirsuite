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

import fi.sn127.utils.fs.{FileUtils, FindFilesPattern, Glob}

@SuppressWarnings(Array(
  "org.wartremover.warts.Null",
  "org.wartremover.warts.DefaultArguments"))
class TestRunnerException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause) {}

@SuppressWarnings(Array(
  "org.wartremover.warts.Null",
  "org.wartremover.warts.DefaultArguments"))
class ExecutionException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause) {}

@SuppressWarnings(Array(
  "org.wartremover.warts.Null",
  "org.wartremover.warts.DefaultArguments"))
class TestVectorException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause) {}


final case class TestVector(reference: Path, output: Path, comparator: (Path, Path) => Boolean)

@SuppressWarnings(Array("org.wartremover.warts.ToString"))
final case class TestCase(cmds: Path, cmdsAndArgs: Seq[Array[String]], testVectors: Seq[TestVector]){
  val name: String = cmds.toString
}


@SuppressWarnings(Array(
  "org.wartremover.warts.ToString",
  "org.wartremover.warts.NonUnitStatements"))
trait TestRunnerLike {

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

  def findReferences(testdir: Path, testname: Path) : Seq[Path] = {
    val fu = FileUtils(testdir.getFileSystem)
    val basename = fu.getBasename(testname) match {case (name, ext) => name}

    fu.findFiles(testdir, Glob(basename + ".ref.*"))
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

  def testExecutorRegister(testPattern: FindFilesPattern, tc: TestCase, specimen: (Array[String] => Any)) = {
    testExecutor(tc, specimen)
  }

  def testIgnoreRegister(dirPatter: String, cmds: Path) = {
    ; // no-op
  }

  def ignoreDirSuite(basedir: Path, dirPattern: FindFilesPattern)(specimen: (Array[String] => Any)) = {
    val fu = FileUtils(basedir.getFileSystem)
    val testnames = fu.findFiles(basedir, dirPattern)

    testnames.foreach(test => testIgnoreRegister(dirPattern.toString, test))
  }

  /**
   * This function should catch all exceptions which are potentially thrown by specimen
   *
   * @param basedir    name of dir which holds test dirs
   * @param testPattern pattern of name of test names
   * @param specimen   returns false if execution failed
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
      testExecutorRegister(testPattern, tc, specimen)
    }
  }

  @SuppressWarnings(Array(
    "org.wartremover.warts.Any"))
  def testExecutor(tc: TestCase, specimen: (Array[String] => Any)) = {
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

      try {
        val v = specimen(argsFeeder(tc.cmds, args))
      } catch {
        case ex: Exception =>
          throw new ExecutionException(execFailMsg +
            " " * 3 + "Exception: \n" +
            " " * 6 + "message: " + ex.getMessage + "\n")
            //, ex)
      }
    }
    for (testVector <- tc.testVectors) {
      def makeComparatorErrMsg(prefix: String) = {
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
          Some(makeComparatorErrMsg(testVectorFailureMsgPrefix))
        }
      } catch {
        case ex: Exception =>
          Some(
            makeComparatorErrMsg(testVectorExceptionMsgPrefix) + "\n" +
              "Exception: \n" +
              "   message: " + ex.getMessage + "\n"
          )
      }
      // NOTE: Collect all comp results, and report all end results together (Cats ...)?
      if (compErrorMsg.nonEmpty) {
        throw  new TestVectorException(compErrorMsg.getOrElse("Internal error in test framework (ex)!"))
      }
    }
  }
}
