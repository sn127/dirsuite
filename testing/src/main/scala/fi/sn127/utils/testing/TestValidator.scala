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

import org.scalactic.TripleEquals._
import org.scalatest.StreamlinedXmlEquality._

@SuppressWarnings(Array("org.wartremover.warts.ToString"))
object TestValidator {

  def txtValidator(testname: Path, reference: Path, output: Path): Option[String] = {
    // TODO: Scala-ARM, and io.File?
    val srcFirst = scala.io.Source.fromFile(output.toString)
    val txtFirst = try srcFirst.mkString  finally srcFirst.close()

    val srcSecond = scala.io.Source.fromFile(reference.toString)
    val txtSecond = try srcSecond.mkString  finally srcSecond.close()

    if (txtFirst === txtSecond) {
      None
    } else {
      Some("test vectors are different")
    }
  }

  def xmlValidator(testname: Path, reference: Path, output: Path): Option[String] = {
    val xmlReference = scala.xml.XML.loadFile(reference.toString)
    val xmlOutput = scala.xml.XML.loadFile(output.toString)

    if (xmlReference === xmlOutput) {
      None
    } else {
      Some("test vectors are different")
    }
  }
}
