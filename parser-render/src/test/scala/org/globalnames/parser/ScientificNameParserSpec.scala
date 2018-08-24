package org.globalnames.parser

import org.json4s._
import org.json4s.jackson.JsonMethods.{parse, pretty}
import spray.json._
import org.specs2.mutable.Specification
import scalaz.std.string._
import scalaz.syntax.std.option._

import scala.io.Source

class ScientificNameParserSpec extends Specification with TestParserInstance {
  case class ExpectedName(verbatim: String, json: String, simple: String)

  "ScientificNameParser specification".p

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
    val jsonExpected = parse(expectedName.json)
    val jsonStrByParser = {
      import formatters.SummarizerProtocol.summaryFormat
      scientificNameParser.fromString(expectedName.verbatim).summary().toJson.prettyPrint
    }
    val jsonByParser = parse(jsonStrByParser)

    def jsonDiff() = {
      val Diff(changed, added, deleted) = jsonByParser.diff(jsonExpected)
      s"""line:
         |${expectedName.verbatim}
         |===> by parser:
         |$jsonStrByParser
         |===> from test data:
         |${pretty(jsonExpected)}
         |changed:
         |${pretty(changed)}
         |added:
         |${pretty(added)}
         |deleted:
         |${pretty(deleted)}""".stripMargin
    }

    s"parse correctly: '${expectedName.verbatim}'" in {
      s"original json must match expected one:\n ${jsonDiff()}" ==> {
        jsonExpected === jsonByParser
      }
    }

    val Array(uuid, verbatim, canonical, canonicalExtended,
              authorship, year, quality) = expectedName.simple.split('|')

    s"parse correctly delimited string: '${expectedName.verbatim}'" in {
      val pr = scientificNameParser.fromString(expectedName.verbatim)

      uuid              === pr.result.preprocessorResult.id.toString
      verbatim          === pr.result.preprocessorResult.verbatim
      canonical         === pr.result.canonical.map { _.value }.orZero
      canonicalExtended === pr.result.canonical.map { _.ranked }.orZero
      authorship        === pr.delimitedStringRenderer.authorshipDelimited.orZero
      year              === pr.delimitedStringRenderer.yearDelimited.orZero
      quality.toInt     === pr.result.scientificName.quality
    }

    s"contain no duplicates in warnings" in {
      val pr = scientificNameParser.fromString(expectedName.verbatim)
      Set(pr.result.warnings: _*).size === pr.result.warnings.size
    }
  }
}
