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

import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.nio.file.{FileSystems, Files, Paths}

import org.scalatest.{FlatSpec, Inside, Matchers}

import fi.sn127.utils.fs.FileUtils

@SuppressWarnings(Array("org.wartremover.warts.Var",
  "org.wartremover.warts.ToString"))
class TestRunnerTest extends FlatSpec with Matchers with Inside {
  val filesystem = FileSystems.getDefault
  val testdir = filesystem.getPath("tests/testrunner").toAbsolutePath.normalize
  val fu = FileUtils(filesystem)

  object DummyApp {
    val SUCCESS = 1
    val FAILED = 0

    def mainSuccess(args: Array[String]): Int = {
      SUCCESS
    }

    def mainFail(args: Array[String]): Int = {
      FAILED
    }

    def mainNothing(args: Array[String]): Int = {
      args.length
    }

    def mainTxt(args: Array[String]): Int = {
      val path =
        Files.write(
          Paths.get(testdir.toString, args(0)),
          args.mkString("hello\n", "\n", "\nworld\n").getBytes(StandardCharsets.UTF_8))
      SUCCESS
    }

    def mainXml(args: Array[String]): Int = {
      val path =
        Files.write(
          Paths.get(testdir.toString, args(0)),
          args.mkString("<hello><arg>", "</arg><arg>", "</arg></hello>\n").getBytes(StandardCharsets.UTF_8))
      SUCCESS
    }

    def mainTxtXml(args: Array[String]): Int = {
      val result =
        if (args(1) === "txt") {
          mainTxt(args)
        } else if (args(1) === "xml") {
          mainXml(args)
        } else {
          FAILED
        }
      result
    }
  }


  behavior of "TestRunner"
  it must "work without output files" in {
    var runCount = 0
    TestRunner.run(testdir,
      "success/tr[0-9]+\\.cmds$",
      (args: Array[String]) => {
        runCount = runCount + 1
        val argCount = DummyApp.mainNothing(args)
        argCount == 4
      })
    assert(runCount === 3)
  }

  it must "detect plain execution errors" in {
    assertThrows[AssertionError] {
      TestRunner.run(testdir,
        "failure/tr[0-9]+\\.cmds$",
        (args: Array[String]) => {
          DummyApp.FAILED == DummyApp.mainSuccess(args)
        })
    }
  }
  it must "work with valid txt-output files" in {
    var runCount = 0
    TestRunner.run(testdir,
      "success/txt[0-9]+\\.cmds$",
      (args: Array[String]) => {
        runCount = runCount + 1
        DummyApp.SUCCESS == DummyApp.mainTxt(args)
      })
    assert(runCount === 2)
  }
  it must "work with valid xml-output files" in {
    var runCount = 0
    TestRunner.run(testdir,
      "success/xml[0-9]+\\.cmds$",
      (args: Array[String]) => {
        runCount = runCount + 1
        DummyApp.SUCCESS == DummyApp.mainXml(args)
      })
    assert(runCount === 1)
  }

  it must "work with valid xml and txt -output files at the same time" in {
    var runCount = 0
    TestRunner.run(testdir,
      "success/txtxml[0-9]+\\.cmds$",
      (args: Array[String]) => {
        runCount = runCount + 1
        DummyApp.SUCCESS == DummyApp.mainTxtXml(args)
      })
    assert(runCount === 2)
  }


  it must "not work with missing files" in {
    assertThrows[FileNotFoundException] {
      TestRunner.run(testdir,
        "failure/missing[0-9]+\\.cmds$",
        (args: Array[String]) => {
          DummyApp.SUCCESS == DummyApp.mainSuccess(args)
        })
    }
  }
  it must "not work with erroneous txt output" in {
    assertThrows[AssertionError] {
      TestRunner.run(testdir,
        "failure/txt[0-9]+\\.cmds$",
        (args: Array[String]) => {
          DummyApp.SUCCESS == DummyApp.mainTxt(args)
        })
    }
  }
  it must "not work with erroneous xml output" in {
    assertThrows[AssertionError] {
      TestRunner.run(testdir,
        "failure/xml[0-9]+\\.cmds$",
        (args: Array[String]) => {
          DummyApp.SUCCESS == DummyApp.mainXml(args)
        })
    }
  }
}
