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

import java.nio.file.{FileSystems, Path, Paths}

import org.scalatest.PrivateMethodTester.PrivateMethod
import org.scalatest.{FlatSpec, Inside, Matchers, PrivateMethodTester}

@SuppressWarnings(Array(
  "org.wartremover.warts.ToString",
  "org.wartremover.warts.NonUnitStatements"))
class BasepathGlobTest extends FlatSpec with Matchers with Inside with PrivateMethodTester {
  val basepath = Paths.get("/foo/bar/")
  val fu = FileUtils(basepath.getFileSystem)
  val basepathGlob = PrivateMethod[FileUtils]('basepathGlob)

  "basepathGlob" should "leave abs path alone" in {
    val doNotTouch = "glob:/some/abs/path/*.txt"

    fu invokePrivate basepathGlob(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "leave relative paths alone" in {
    val doNotTouch = "glob:../*.txt"

    fu invokePrivate basepathGlob(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "leave alone glob starting with '*'" in {
    val doNotTouch = "glob:*.txt"

    fu invokePrivate basepathGlob(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "leave alone glob starting with '**/'" in {
    val doNotTouch = "glob:**/*.txt"

    fu invokePrivate basepathGlob(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "leave alone glob starting with '?'" in {
    val doNotTouch = "glob:?oo.txt"

    fu invokePrivate basepathGlob(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "leave alone glob starting with '{'" in {
    val doNotTouch = "glob:{sun,moon,stars}"

    fu invokePrivate basepathGlob(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "leave alone glob starting with '['" in {
    val doNotTouch = "glob:[A-Z]"

    fu invokePrivate basepathGlob(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "prefix plain glob with absolute path" in {
    val doNotTouch = "glob:a/*.txt"

    fu invokePrivate basepathGlob(doNotTouch, fu.getCanonicalPath(basepath)) should
      equal("glob:" + basepath.toString + "/a/*.txt")
  }
}

@SuppressWarnings(Array(
  "org.wartremover.warts.ToString",
  "org.wartremover.warts.NonUnitStatements"))
class BasepathRegexTest extends FlatSpec with Matchers with Inside with PrivateMethodTester {
  val basepath = Paths.get("/foo/bar/")
  val fu = FileUtils(basepath.getFileSystem)
  val basepathRegex = PrivateMethod[FileUtils]('basepathRegex)

  "basepathRegex" should "leave abs path alone" in {
    val doNotTouch = "regex:/some/abs/path/.*\\.txt"

    fu invokePrivate basepathRegex(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "leave relative paths alone" in {
    val doNotTouch = "regex:\\.\\./.*\\.txt"

    fu invokePrivate basepathRegex(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "leave backslash alone 'e.g. \\d'" in {
    val doNotTouch = "regex:\\d+/.*\\.txt"

    fu invokePrivate basepathRegex(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "leave character classes alone '['" in {
    val doNotTouch = "regex:[a-z]+/.*\\.txt"

    fu invokePrivate basepathRegex(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "leave match anything alone ('.*')" in {
    val doNotTouch = "regex:.*/.*\\.txt"

    fu invokePrivate basepathRegex(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "leave begin of string alone ('^')" in {
    val doNotTouch = "regex:^2015/.*\\.txt"

    fu invokePrivate basepathRegex(doNotTouch, basepath) should equal(doNotTouch)
  }
  it should "leave end of string alone ('$')" in {
    val doNotTouch = "regex:$"

    fu invokePrivate basepathRegex(doNotTouch, basepath) should equal(doNotTouch)
  }

  it should "leave begin of string alone ('(')" in {
    val doNotTouch = "regex:((2015)|(2014))/.*\\.txt"

    fu invokePrivate basepathRegex(doNotTouch, basepath) should equal(doNotTouch)
  }


  it should "prefix plain regex with absolute path" in {
    val doNotTouch = "regex:a/.*\\.txt"

    fu invokePrivate basepathRegex(doNotTouch, fu.getCanonicalPath(basepath)) should
      equal("regex:" + java.util.regex.Pattern.quote(
        basepath.toString + FileSystems.getDefault.getSeparator
      ) + "a/.*\\.txt")
  }
}

@SuppressWarnings(Array(
  "org.wartremover.warts.ToString",
  "org.wartremover.warts.NonUnitStatements",
  "org.wartremover.warts.Equals"))
class WildcardInputTest extends FlatSpec with Matchers with Inside {

  val testDirPath = Paths.get("tests/globtree").toAbsolutePath.normalize()
  val fu = FileUtils(testDirPath.getFileSystem)

  /*
   * This tests expect working directory to be top level project dir.
   * Take this into account, If you are running these test from IDE.
   */
  private def verify(paths: Seq[Path], refPaths: Seq[String], baseDir: Path) = {

    (paths.length == refPaths.length) &&
      paths.nonEmpty &&
      paths.sorted.zip(refPaths.map(refPath => {
        fu.getPath(fu.getCanonicalPath(baseDir).toString, refPath)
      }).sorted)
        .forall({
          case (path, refPath) =>
            if (path == refPath) {
              true
            }
            else {
              println("path: " + path.toString)
              println("ref:  " + refPath.toString)
              false
            }
        })
  }

  private def printpaths(ps: Seq[Path]) = {
    println("SIZE: " + "%d".format(ps.size))
    ps.foreach(p => {
      println("PATH: " + p.toString)
    })
  }

  "glob" should "match plain file (e.g. 'file.ext')" in {
    val refPaths = Seq(
      "one.txt")

    val paths = fu.globFindFiles(testDirPath, "one.txt")

    verify(paths, refPaths, testDirPath) should be(true)
  }

  it should "match path without glob (e.g. 'subdir/foo/file.ext')" in {
    val refPaths = Seq(
      "a/a.txt")

    val paths = fu.globFindFiles(testDirPath, "a/a.txt")

    verify(paths, refPaths, testDirPath) should be(true)
  }

  it should "match file-glob (e.g. '*.ext')" in {
    val refPaths = Seq(
      "one.txt",
      "two.txt",
      "three.txt")

    val paths = fu.globFindFiles(testDirPath, "**/globtree/*.txt")

    verify(paths, refPaths, testDirPath) should be(true)
  }

  it should "match sub dir file-glob  (e.g. 'subdir/*.ext')" in {
    val refPaths = List(
      "a/a.txt",
      "a/x.txt")

    val paths = fu.globFindFiles(testDirPath, "a/*.txt")

    verify(paths, refPaths, testDirPath) should be(true)
  }

  it should "use parentdir for matching (e.g. plain 'subdir/*.ext' instead of '**/subdir/*.ext)" in {
    // e.g. check that b nor c are matched, nor b/a
    val refPaths = Seq(
      "a/a.txt",
      "a/x.txt")

    val paths = fu.globFindFiles(testDirPath, "a/*.txt")

    verify(paths, refPaths, testDirPath) should be(true)
  }

  it should "match sub-directory glob with plain file (e.g. 'subdir/*/file.ext')" in {
    val refPaths = Seq(
      "a/x.txt",
      "c/x.txt")

    val paths = fu.globFindFiles(testDirPath, "**/globtree/*/x.txt")

    verify(paths, refPaths, testDirPath) should be(true)
  }

  it should "match sub-directory glob with file-glob (e.g. 'subdir/*/*.ext')" in {
    val refPaths = Seq(
      "a/a.txt",
      "a/x.txt",
      "c/c.txt",
      "c/x.txt",
      "b/b.txt")

    val paths = fu.globFindFiles(testDirPath, "**/globtree/*/*.txt")

    verify(paths, refPaths, testDirPath) should be(true)
  }

  it should "match deep sub-directory glob with plain file (e.g. 'subdir/**/file.ext')" in {
    val refPaths = Seq(
      "a/a2/x.txt",
      "a/x.txt",
      "c/x.txt")

    val paths = fu.globFindFiles(testDirPath, "**/globtree/**/x.txt")

    verify(paths, refPaths, testDirPath) should be(true)
  }

  it should "match deep sub-directory glob with file-glob (e.g. 'subdir/**/*.ext')" in {
    val refPaths = Seq(
      "a/a.txt",
      "a/x.txt",
      "a/a2/x.txt",
      "a/a2/a2.txt",
      "c/x.txt",
      "c/c.txt",
      "b/b.txt",
      "b/a/ba.txt")

    val paths = fu.globFindFiles(testDirPath, "**/globtree/**/*.txt")

    verify(paths, refPaths, testDirPath) should be(true)
  }

  it should "match everything (e.g. 'subdir/**')" in {
    val refPaths = Seq(
      "one.txt",
      "a/a.txt",
      "a/x.txt",
      "a/a2/x.txt",
      "a/a2/a2.txt",
      "a/a.not",
      "two.txt",
      "three.txt",
      "c/x.txt",
      "c/c.txt",
      "b/b.txt",
      "b/a/ba.txt",
      "readme.md")

    val paths = fu.globFindFiles(testDirPath, "**/globtree/**")

    verify(paths, refPaths, testDirPath) should be(true)
  }

  "Regex" should "match all txt-files  (e.g. '.*/.*\\\\.txt')" in {
    val refPaths = Seq(
      "one.txt",
      "a/a.txt",
      "a/x.txt",
      "a/a2/x.txt",
      "a/a2/a2.txt",
      "two.txt",
      "three.txt",
      "c/x.txt",
      "c/c.txt",
      "b/b.txt",
      "b/a/ba.txt")

    val paths = fu.regexFindFiles(testDirPath, ".*/.*\\.txt")

    verify(paths, refPaths, testDirPath) should be(true)
  }

  it should "use parentdir for matching (e.g. plain 'subdir/*.ext' instead of '**/subdir/*.ext)" in {
    // e.g. check that b nor c are matched, nor b/a
    val refPaths = Seq(
      "a/a.txt",
      "a/x.txt",
      "a/a2/a2.txt",
      "a/a2/x.txt")

    val paths = fu.regexFindFiles(testDirPath, "a/.*\\.txt")

    verify(paths, refPaths, testDirPath) should be(true)
  }
}
