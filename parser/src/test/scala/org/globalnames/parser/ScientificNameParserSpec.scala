package org.globalnames.parser

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.specs2.mutable.Specification

import scala.io.Source
import scalaz.std.string._
import scalaz.syntax.std.option._

class ScientificNameParserSpec extends Specification {
  case class ExpectedName(verbatim: String, json: String, simple: String)

  "ScientificNameParser specification".p

  val scientificNameParser = new ScientificNameParser {
    val version = "test_version"
  }

  def expectedNames(filePath: String): Vector[ExpectedName] = {
    Source.fromURL(getClass.getResource(filePath), "UTF-8")
          .getLines
          .takeWhile { _.trim != "__END__" }
          .withFilter { line => !(line.isEmpty || ("#\r\n\f\t" contains line.charAt(0))) }
          .sliding(3, 3)
          .map { ls => ExpectedName(ls(0), ls(1), ls(2)) }
          .toVector
  }

  expectedNames("/test_data.txt").foreach { expectedName =>
      val json = parse(expectedName.json)
      val jsonParsed = scientificNameParser.fromString(expectedName.verbatim).json()
                         .removeField { case (_, v) => v == JNothing }

      val jsonDiff = {
        val Diff(changed, added, deleted) = jsonParsed.diff(json)
        s"""line:
           |${expectedName.verbatim}
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

      s"parse correctly: '${expectedName.verbatim}'" in {
        s"original json must match expected one:\n $jsonDiff" ==> {
          json === jsonParsed
        }
      }

      val Array(uuid, verbatim, canonical, canonicalExtended,
                authorship, year, quality) = expectedName.simple.split('|')

      s"parse correctly delimited string: '${expectedName.verbatim}'" in {
        val pr = scientificNameParser.fromString(expectedName.verbatim)

        uuid              === pr.preprocessorResult.id.toString
        verbatim          === pr.preprocessorResult.verbatim
        canonical         === pr.canonized().orZero
        canonicalExtended === pr.canonized(showRanks = true).orZero
        authorship        === pr.authorshipDelimited.orZero
        year              === pr.yearDelimited.orZero
        quality.toInt     === pr.scientificName.quality
      }

      s"contain no duplicates in warnings" in {
        val pr = scientificNameParser.fromString(expectedName.verbatim)
        Set(pr.warnings: _*).size === pr.warnings.size
      }
  }
}
