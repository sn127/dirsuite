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
package fi.sn127.utils.fs

import java.nio.file.FileSystems

import org.scalatest.{FlatSpec, Inside, Matchers}

@SuppressWarnings(Array(
  "org.wartremover.warts.ToString",
  "org.wartremover.warts.NonUnitStatements"))
class FileUtilsTest extends FlatSpec with Matchers with Inside {

  val filesystem = FileSystems.getDefault
  val testDirPath = filesystem.getPath("tests/globtree").toAbsolutePath.normalize
  val fu = FileUtils(filesystem)

  "getCanonicalDirPath" must "work" in {
    fu.getCanonicalPath(filesystem.getPath("/")).toString should be("/")

    fu.getCanonicalPath(filesystem.getPath("/foo/./bar")).toString should be("/foo/bar")
    fu.getCanonicalPath(filesystem.getPath("/foo/../bar")).toString should be("/bar")
    fu.getCanonicalPath(filesystem.getPath("/foo/bar/..")).toString should be("/foo")
    fu.getCanonicalPath(filesystem.getPath("./")).toString should be(
      filesystem.getPath(".").toAbsolutePath.normalize.toString)
  }

  "getParentDirPath" must "work" in {
    fu.getParentDirPath(filesystem.getPath("/foo/bar")).toString should be("/foo")
    fu.getParentDirPath(filesystem.getPath("/foo/bar/")).toString should be("/foo")

    // TODO Check and fix semantics of getParentDirPath with these corner cases
    fu.getParentDirPath(filesystem.getPath("/")).toString should be("")
    fu.getParentDirPath(filesystem.getPath(".")).toString should be("")
    fu.getParentDirPath(filesystem.getPath("..")).toString should be("")
  }

  "getPathWithAnchor" must "work" in {
    fu.getPathWithAnchor("/foo/bar", filesystem.getPath("/snafu")).toString should be("/foo/bar")

    fu.getPathWithAnchor("./f.txt", filesystem.getPath("/snafu/foo/bar/ref.txt")).toString should
      be("/snafu/foo/bar/f.txt")

    fu.getPathWithAnchor("../f.txt", filesystem.getPath("/snafu/foo/bar/ref.txt")).toString should
      be("/snafu/foo/f.txt")

    fu.getPathWithAnchor("../b.txt", filesystem.getPath(testDirPath.toString, "b/a")).toString should
      be(testDirPath.toString + "/b/b.txt")
  }

  "ensurePath" must "find file" in {
    fu.ensurePath(testDirPath.toString + "/one.txt") match {
      case Some(path) => assert(path.toString === testDirPath.toString + "/one.txt")
      case None => assert(false)
    }
  }

  it must "not find file" in {
    fu.ensurePath(testDirPath.toString + "/it-is-not-there.txt") match {
      case Some(path) => assert(false)
      case None =>
    }
  }
}