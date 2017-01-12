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

import java.nio.charset.StandardCharsets
import java.nio.file.{FileSystems, Files, Path}

import org.scalatest.exceptions.TestFailedException
import org.scalatest.{FlatSpec, Inside, Matchers}

import fi.sn127.utils.fs.{FileUtils, FindFilesPattern, Glob, Regex}

class TestRunner extends TestRunnerLike {
  override
  def registerDirSuiteTest(pattern: FindFilesPattern, tc: TestCase, specimen: (Array[String]) => Any) = {
      testExecutor(tc, specimen)
  }

  override
  def registerIgnoredDirSuiteTest(dirPattern: String, cmdsPath: Path) = {
    ;
  }
}

@SuppressWarnings(Array("org.wartremover.warts.Var",
  "org.wartremover.warts.ToString"))
class TestRunnerTest extends FlatSpec with Matchers with Inside {
  val filesystem = FileSystems.getDefault
  val testdir = filesystem.getPath("tests/testrunner").toAbsolutePath.normalize
  val fu = FileUtils(filesystem)

  val testRunner = new TestRunner

  object DummyApp {
    val SUCCESS = 1
    val FAILURE = -1

    def mainSuccess(args: Array[String]): Int = {
      SUCCESS
    }

    def mainFail(args: Array[String]): Int = {
      FAILURE
    }

    def mainArgsCount(args: Array[String]): Int = {
      // Console.err.println("running with args: " + "%d".format(args.length))
      args.length
    }

    def mainTxt(args: Array[String]): Int = {
      val path =
        Files.write(fu.getPath(testdir.toString, args(0)),
          args
            .mkString("hello\n", "\n", "\nworld\n")
            .getBytes(StandardCharsets.UTF_8))
      SUCCESS
    }

    def mainXml(args: Array[String]): Int = {
      val path =
        Files.write(fu.getPath(testdir.toString, args(0)),
          args
            .mkString("<hello><arg>", "</arg><arg>", "</arg></hello>\n")
            .getBytes(StandardCharsets.UTF_8))
      SUCCESS
    }

    def mainTxtXml(args: Array[String]): Int = {
      val result =
        if (args(1) === "txt") {
          mainTxt(args)
        } else if (args(1) === "xml") {
          mainXml(args)
        } else {
          FAILURE
        }
      result
    }
  }

  behavior of "ignoreDirSuite"
  it must "ignore files" in {
    var runCount = 0
    testRunner.ignoreDirSuite(testdir, Regex("success/tr[0-9]+\\.cmds")) { args: Array[String] =>
      assertResult(4) {
        runCount = runCount + 1
        DummyApp.mainArgsCount(args)
      }
    }
    assert(runCount === 0)
  }

  behavior of "runDirSuite"
  it must "work with empty execution args cmds file (e.g. rows of plain ';'s)" in {
    var runCount = 0
    testRunner.runDirSuite(testdir, Regex("success/noargs[0-9]+\\.cmds")) { args: Array[String] =>
      assertResult(0) {
        runCount = runCount + 1
        DummyApp.mainArgsCount(args)
      }
    }
    assert(runCount === 2)
  }
  it must "work globs" in {
    var runCount = 0
    testRunner.runDirSuite(testdir, Glob("success/noargs*.cmds")) { args: Array[String] =>
      assertResult(0) {
        runCount = runCount + 1
        DummyApp.mainArgsCount(args)
      }
    }
    assert(runCount === 2)
  }

  it must "work without output files" in {
    var runCount = 0
    testRunner.runDirSuite(testdir, Regex("success/tr[0-9]+\\.cmds")) { args: Array[String] =>
      assertResult(4) {
        runCount = runCount + 1
        DummyApp.mainArgsCount(args)
      }
    }
    assert(runCount === 3)
  }

  it must "work with valid txt-output files" in {
    var runCount = 0
    testRunner.runDirSuite(testdir, Regex("success/txt[0-9]+\\.cmds")) { args: Array[String] =>
      assertResult(DummyApp.SUCCESS) {
        runCount = runCount + 1
        DummyApp.mainTxt(args)
      }
    }
    // two txt[0-9] cmds  files, each have one exec-row
    assert(runCount === 2)
  }

  it must "work with valid xml-output files" in {
    var runCount = 0
    testRunner.runDirSuite(testdir, Regex("success/xml[0-9]+\\.cmds")) { args: Array[String] =>
      assertResult(DummyApp.SUCCESS) {
        runCount = runCount + 1
        DummyApp.mainXml(args)
      }
    }
    assert(runCount === 1)
  }

  it must "work with valid xml and txt -output files at the same time" in {
    var runCount = 0
    testRunner.runDirSuite(testdir, Regex("success/txtxml[0-9]+\\.cmds")) { args: Array[String] =>
      assertResult(DummyApp.SUCCESS) {
        runCount = runCount + 1
        DummyApp.mainTxtXml(args)
      }
    }
    assert(runCount === 2)
  }

  it must "detect plain execution errors" in {
    val ex = intercept[TestFailedException] {
      testRunner.runDirSuite(testdir, Regex("failure/tr[0-9]+\\.cmds")) { args: Array[String] =>
        assert(DummyApp.SUCCESS == DummyApp.mainFail(args))
      }
    }
    assert(ex.getMessage.startsWith(testRunner.executionFailureMsgPrefix))
  }

  it must "detect plain execution errors with assertResult and interceptor" in {
    val ex = intercept[TestFailedException] {
      testRunner.runDirSuite(testdir, Regex("failure/tr[0-9]+\\.cmds")) { args: Array[String] =>
        assertResult(DummyApp.SUCCESS) {
          DummyApp.mainFail(args)
        }
      }
    }
    assert(ex.getMessage.startsWith(testRunner.executionFailureMsgPrefix))
  }

  it must "detect missing files" in {
    val ex = intercept[TestVectorException] {
      testRunner.runDirSuite(testdir, Regex("failure/missing[0-9]+\\.cmds")) { args: Array[String] =>
        assertResult(DummyApp.SUCCESS) {
          DummyApp.mainSuccess(args)
        }
      }
    }
    assert(ex.getMessage.startsWith(testRunner.testVectorExceptionMsgPrefix))
  }

  it must "detect erroneous txt output" in {
    val ex = intercept[TestVectorException] {
      testRunner.runDirSuite(testdir, Regex("failure/txt[0-9]+\\.cmds")) { args: Array[String] =>
        assertResult(DummyApp.SUCCESS) {
          DummyApp.mainTxt(args)
        }
      }
    }
    assert(ex.getMessage.startsWith(testRunner.testVectorFailureMsgPrefix))
  }

  it must "detect erroneous xml output" in {
    val ex = intercept[TestVectorException] {
      testRunner.runDirSuite(testdir, Regex("failure/xml[0-9]+\\.cmds")) { args: Array[String] =>
        assertResult(DummyApp.SUCCESS) {
          DummyApp.mainXml(args)
        }
      }
    }
    assert(ex.getMessage.startsWith(testRunner.testVectorFailureMsgPrefix))
  }

  it must "detect and report comparator exceptions (XML SAX, JSON, etc)" in {
    val ex = intercept[TestVectorException] {
      testRunner.runDirSuite(testdir, Regex("failure/xml-sax[0-9]+\\.cmds")) { (args: Array[String]) =>
        assertResult(DummyApp.SUCCESS) {
          DummyApp.mainXml(args)
        }
      }
    }
    assert(ex.getMessage.startsWith(testRunner.testVectorExceptionMsgPrefix))
  }
}
