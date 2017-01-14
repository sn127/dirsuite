/*
 * Copyright 2017 Jani Averbach
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

import java.nio.file.FileSystems

import fi.sn127.utils.fs.{Glob, Regex}

object DemoProg {
  val SUCCESS = 1
  val FAILURE = 0

  def mainSuccess(args: Array[String]): Int = {
    SUCCESS
  }

  def mainFailure(args: Array[String]): Int = {
    FAILURE
  }
}

class DemoDirSuiteTest extends DirSuite {

  val filesystem = FileSystems.getDefault
  val testdir = filesystem.getPath("tests/dirsuite").toAbsolutePath.normalize

  ignoreDirSuite(testdir, Regex("success/txt[0-9]+\\.exec")) { args: Array[String] =>
    assertResult(DemoProg.SUCCESS) {
      DemoProg.mainSuccess(args)
    }
  }

  ignoreDirSuite(testdir, Glob("success/tr*.exec")) { args: Array[String] =>
    assertResult(DemoProg.SUCCESS) {
      DemoProg.mainFailure(args)
    }
  }
}
