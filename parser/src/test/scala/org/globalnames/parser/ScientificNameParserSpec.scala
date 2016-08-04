package org.globalnames.parser

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.specs2.mutable.Specification

import scala.io.Source

class ScientificNameParserSpec extends Specification {

  "ScientificNameParser specification".p

  val scientificNameParser = new ScientificNameParser {
    val version = "test_version"
  }

  def expectedNames(filePath: String): Vector[(String, String, String)] = {
    Source.fromURL(getClass.getResource(filePath), "UTF-8")
          .getLines
          .takeWhile { _.trim != "__END__" }
          .withFilter { line =>
           !(line.isEmpty || ("#\r\n\f\t" contains line.charAt(0)))
          }
          .sliding(3, 3)
          .map { ls => (ls(0), ls(1), ls(2)) }
          .toVector
  }

  expectedNames("/test_data.txt").foreach {
    case (inputStr, expectedJsonStr, expectedDelimitedStr) =>
      val json = parse(expectedJsonStr)
      val jsonParsed = scientificNameParser.fromString(inputStr).json()
                         .removeField { case (_, v) => v == JNothing }

      val jsonDiff = {
        val Diff(changed, added, deleted) = jsonParsed.diff(json)
        s"""line:
           |$inputStr
           |parsed:
           |${pretty(jsonParsed)}
           |test_data:
           |${pretty(json)}
           |changed:
           |${pretty(changed)}
           |added:
           |${pretty(added)}
           |deleted:
           |${pretty(deleted)}""".stripMargin
      }

      s"parse correctly: '$inputStr'" in {
        s"original json must match expected one:\n $jsonDiff" ==> {
          json === jsonParsed
        }
      }

      val Array(uuid, verbatim, canonical, canonicalExtended,
                authorship, year, quality) = expectedDelimitedStr.split('|')

      s"parse correctly delimited string: '$inputStr'" in {
        val pr = scientificNameParser.fromString(inputStr)

        uuid              === pr.input.id.toString
        verbatim          === pr.input.verbatim
        canonical         === pr.canonized().getOrElse("")
        canonicalExtended === pr.canonized(showRanks = true).getOrElse("")
        authorship        === pr.authorshipDelimited
        year              === pr.yearDelimited
        quality.toInt     === pr.scientificName.quality
      }
  }
}
