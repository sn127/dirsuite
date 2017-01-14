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

import java.io.IOException
import java.nio.file.{FileSystem, Files, Path, PathMatcher, Paths}
import java.util.regex.Pattern

import resource._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


sealed trait FindFilesPattern

/**
 * Glob for path matching.
 *
 * For syntax see:
 * [[https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob]]
 * @param glob string *without* "glob:"
 */
final case class Glob(glob: String) extends FindFilesPattern {
  override def toString: String = {
    "Glob(" + glob + ")"
  }
}

/**
 * Regex for path matching.
 *
 * For syntax see:
 * [[https://docs.oracle.com/javase/tutorial/essential/regex/index.html]]
 * @param regex string *without* "regex:"
 */
final case class Regex(regex: String) extends FindFilesPattern {
  override def toString: String = {
    "Regex(" + regex + ")"
  }
}

/**
 * Various filesystem and path related utilities.
 *
 * @param filesystem this is used as default filesystem
 *                   for path manipulation operations in
 *                   this instance.
 */
@SuppressWarnings(Array("org.wartremover.warts.ToString"))
final case class FileUtils(filesystem: FileSystem) {

  /**
   * Converts a string path or sequence of strPaths to
   * path. See: java.nio.Filesystem.getPath
   *
   * @param first
   * @param more
   * @return combined path
   */
  def getPath(first: String, more: String*): Path = {
    filesystem.getPath(first, more: _*)
  }

  /**
   * Ensure that path exists.
   *
   * See: java.nio.file.Path#toRealPath
   *
   * @param path to be checked
   * @return real path  or Failure(Exception)
   */
  def ensurePath(path: Path): Try[Path] = {
    try {
      Success[Path](path.toRealPath())
    } catch {
      case ex: IOException => Failure[Path](ex)
    }
  }

  /**
   * Returns exepath (\$0) of current class or jar
   *
   * @param c class which is used to determine execution path
   * @return full path of class or jar
   */
  def getExePath(c: Class[_]): Path = {
    // Paths.get is ok-ish, because this is URI already
    // -> it is game over with filesystem (it is what it is)
    Paths.get(c.getProtectionDomain.getCodeSource.getLocation.toURI)
  }

  /**
   * Return execution directory of current class or jar
   *
   * @param c class which is used to determine execution path
   * @return full path of directory of class or jar
   */
  def getExeDir(c: Class[_]): Path = {
    val runRawPath = getExePath(c)
    if (Files.isDirectory(runRawPath))
      runRawPath
    else if (Files.isRegularFile(runRawPath))
      runRawPath.getParent
    else
      throw new RuntimeException("Unknown file type with path: [" + runRawPath.toString + "]")
  }

  /**
   * Get basename (and extension) of path.
   * Basename is filename's body part with last,
   * dot-separated extension removed.
   *
   * @param path
   * @return (basename, and optional extension)
   */
  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  def getBasename(path: Path): (String, Option[String]) = {
    val result: Array[String] = path.getFileName.toString.reverse.split("\\.", 2)

    if (result.size == 2) {
      (result(1).reverse,
        Some(result(0).reverse))
    } else {
      (result(0).reverse, None)
    }
  }

  /**
   * Make canonical path of path
   *
   * @param path to existing fs path
   * @return absolute and normalized path
   */
  def getCanonicalPath(path: Path): Path = {
    path.toAbsolutePath.normalize
  }

  /**
   * Get canonical parent path of anchor
   *
   * @param anchor path
   * @return canonical parent path of anchor or empty path
   */
  def getParentDirPath(anchor: Path): Path = {
    Option(anchor.getParent) match {
      case Some(parentDir) => getCanonicalPath(parentDir)
      case None => filesystem.getPath("")
    }
  }


  /**
   * Get absolute path with help of reference anchor
   * Anchor is used as reference in case that path
   * is not absolute and it could be path to directory or
   * file. If it's file, then file's parent dir is used
   * as an anchor.
   *
   * If type of anchor is unknown, then it is treated as
   * file. One reason for that is if path is not a real,
   * existing path on filesystem.
   *
   * @param pathStr relative or absolute path as string
   * @param anchor  reference path to be used in case pathStr
   *                is not absolute
   * @return absolute and normalized path to pathStr
   */
  def getPathWithAnchor(pathStr: String, anchor: Path): Path = {
    val path = filesystem.getPath(pathStr)
    if (path.isAbsolute) {
      path.normalize
    } else {
      if (Files.isDirectory(anchor)) {
        filesystem.getPath(getCanonicalPath(anchor).toString, path.toString).normalize
      } else {
        filesystem.getPath(getParentDirPath(anchor).toString, path.toString).normalize
      }
    }
  }

  /**
   * Find files which match pattern, under basepath
   * Pattern could be either [[Glob]] or [[Regex]]
   *
   * @param basepath
   * @param pattern
   * @return sequence of paths which matched pattern
   */
  def findFiles(basepath: Path, pattern: FindFilesPattern): Seq[Path] = {
    val canonicalBasepath = getCanonicalPath(basepath)

    val cooked =
      pattern match {
        case glob: Glob =>
          basepathGlob("glob:" + glob.glob, canonicalBasepath)
        case regex: Regex =>
          basepathRegex("regex:" + regex.regex, canonicalBasepath)
      }

    val matcher = filesystem.getPathMatcher(cooked)
    matcherFindFiles(canonicalBasepath, matcher)
  }

  protected def listDirectory(dir: Path): Seq[Path] = {

    // Scala-ARM: managed close
    managed(Files.newDirectoryStream(dir))
      .map(dirStream => {
        // use toList so whole stream will be consumed,
        // because there is funky interaction between
        // scala-ARM and seq (it will return a seq
        // with only one item).
        dirStream.iterator().asScala.toList
      }).opt match {
      case Some(dirList) =>
        dirList
      case None =>
        Nil
    }
  }


  protected def fsEntryMapper(entry: Path): Seq[Path] ={
    if (Files.isRegularFile(entry)) {
      List(entry)
    } else if (Files.isDirectory(entry)) {
      // In theory, this can blow up your stack, but in practice, it won't.
      // To do that, there would have to be very deep directory structure.
      listDirTree(entry)
    } else {
      // Devices etc. (LINKS! - not supported at the moment)
      Nil
    }
  }

  protected def listDirTree(dir: Path): Seq[Path] = {
    listDirectory(dir).flatMap { entry =>
        fsEntryMapper(entry)
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  protected def getEscapedFileSeparatorChar = {
    if (filesystem.getSeparator == "\\") """\\""" else "/"
  }

  /**
   * Cook glob with basepath so that it will match relative path
   * without leading glob-pattern.
   *
   * @param glob
   * @param basepath
   * @return
   */
  protected def basepathGlob(glob: String, basepath: Path) = {
    // globStartChar := '*' | '?' | '{' | '[' | os.pathSep | '.'
    val pathSepRgx = getEscapedFileSeparatorChar
    val regex = "^glob:((\\*)|(\\?)|(\\{)|(\\[)|(" + pathSepRgx + ")|(\\.)).*"

    if (glob.matches(regex)) {
      // it begins with special character -> leave it alone
      glob
    } else {
      val basepathWithSep = basepath.toString + filesystem.getSeparator
      "glob:" + basepathWithSep.replaceAll("""\\""", """\\\\""") + glob.stripPrefix("glob:")
    }
  }

  /**
   * Cook regex with basepath so that it will match relative path
   * without leading regex-pattern.
   */
  protected def basepathRegex(pathRegex: String, basepath: Path) = {
    // regexStartChar := '\' | '[' | '.' | '^' | '$' | '(' | os.pathSep

    val pathSepRgx = getEscapedFileSeparatorChar
    val regex = "^regex:((\\\\)|(\\[)|(\\.)|(\\^)|(\\$)|(\\()|(" + pathSepRgx + ")).*"

    if (pathRegex.matches(regex)) {
      // it begins with special character -> leave it alone
      pathRegex
    } else {
      val basepathWithSep = basepath.toString + filesystem.getSeparator
      "regex:" + Pattern.quote(basepathWithSep) + pathRegex.stripPrefix("regex:")
    }
  }

  protected def matcherFindFiles(canonicalBasepath: Path, matcher: PathMatcher) = {
    listDirTree(canonicalBasepath)
      .filter(f => matcher.matches(f))
      .sortBy(f => f.toString)
  }
}
