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
package fi.sn127.utils.fs

import java.nio.file.FileSystems

import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.util.{Failure, Success}

@SuppressWarnings(Array(
  "org.wartremover.warts.ToString",
  "org.wartremover.warts.NonUnitStatements"))
class FileUtilsTest extends FlatSpec with Matchers with Inside {

  private val filesystem = FileSystems.getDefault
  private val testDirPath = filesystem.getPath("tests/globtree").toAbsolutePath.normalize
  private val fu = FileUtils(filesystem)

  "getExeDir" must "work" in {
    assert(fu.getExeDir(getClass).getFileName.toString === "test-classes")
  }

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


  behavior of "ensurePath"
  it must "find file" in {
    val one = fu.getPath(testDirPath.toString, "/one.txt")
    fu.ensurePath(one) match {
      case Success(path) => assert(path.toString === one.toString)
      case Failure(_) => assert(false)
    }
  }
  it must "not find file" in {
    val nowhere = fu.getPath(testDirPath.toString, "/it-is-not-there.txt")
    fu.ensurePath(nowhere) match {
      case Success(_) => assert(false)
      case Failure(_) =>
    }
  }


  behavior of "getBasename"
  it must "work with file" in {
    val result = fu.getBasename(fu.getPath("abc.txt"))
    assert(result._1 === "abc")
    assert(result._2 === Some("txt"))
  }
  it must "work with file with path" in {
    val result = fu.getBasename(fu.getPath("/foo/bar/abc.txt"))
    assert(result._1 === "abc")
    assert(result._2 === Some("txt"))
  }
  it must "work with multiple dots" in {
    val result = fu.getBasename(fu.getPath("/foo/bar/abc.tar.gz"))
    assert(result._1 === "abc.tar")
    assert(result._2 === Some("gz"))
  }
  it must "work without extension (file)" in {
    val result = fu.getBasename(fu.getPath("abc"))
    assert(result._1 === "abc")
    assert(result._2 === None)
  }
  it must "work without extension (path)" in {
    val result = fu.getBasename(fu.getPath("/foo/bar/abc"))
    assert(result._1 === "abc")
    assert(result._2 === None)
  }
  it must "work with zero ext" in {
    val result = fu.getBasename(fu.getPath("/foo/bar/abc."))
    assert(result._1 === "abc")
    assert(result._2 === Some(""))
  }
  it must "work with unix hidden files" in {
    val result = fu.getBasename(fu.getPath(".vimrc"))
    assert(result._1 === "")
    assert(result._2 === Some("vimrc"))
  }
  it must "work with unix hidden files with path" in {
    val result = fu.getBasename(fu.getPath("/foo/bar/.vimrc"))
    assert(result._1 === "")
    assert(result._2 === Some("vimrc"))
  }
}
