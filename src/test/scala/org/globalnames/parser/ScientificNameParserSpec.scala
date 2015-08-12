package org.globalnames.parser

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.specs2.mutable.Specification

import scala.io.Source

class ScientificNameParserSpec extends Specification {

  "ScientificNameParser specification".p

  val lines = Source.fromURL(getClass.getResource("/test_data.txt"), "UTF-8").getLines
  val scientificNameParser = new ScientificNameParser {
    val version = "test_version"
  }

  for (line <- lines.takeWhile { _.trim != "__END__" } if !(line.isEmpty || ("#\r\n\f\t" contains line.charAt(0)))) {
    val Array(inputStr, expectedJsonStr) = line.split('|')
    val parsed = scientificNameParser.fromString(inputStr)
    parsed.namesGroup !== None

    val json = parse(expectedJsonStr)
                .removeField { case (n, v) => n == "positions" || n == "parser_run" || n == "surrogate" }
    val jsonParsed = scientificNameParser.json(parsed)
    val jsonDiff = {
      val Diff(changed, added, deleted) = jsonParsed.diff(json)
      s"""Line:
         |$line
         |Original:
         |${pretty(jsonParsed)}
         |Expected:
         |${pretty(json)}
         |Changed:
         |${pretty(changed)}
         |Added:
         |${pretty(added)}
         |Deleted:
         |${pretty(deleted)}""".stripMargin
    }

    s"parse correctly: '$inputStr'" in {
      s"original json must match expected one:\n $jsonDiff" ==> {
        json === jsonParsed
      }
    }
  }
}
