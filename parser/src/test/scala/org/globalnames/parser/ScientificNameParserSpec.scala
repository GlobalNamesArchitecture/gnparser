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

    val json1 = parse(expectedJsonStr)
    val json = parse(expectedJsonStr)
    val jsonParsed = scientificNameParser.json(parsed).removeField { case (_, v) => v == JNothing }
    val jsonDiff = {
      val Diff(changed, added, deleted) = jsonParsed.diff(json)

      val r = json1.mapField {
        case (x, y) if x == "uninomial" => "uninomial" -> jsonParsed \\ "uninomial"
        case x => x
      }
      val k = s"""Line:
                 |$line
          |Fixed:
          |$inputStr|${compact(r)}
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
      if (changed != JNothing || added != JNothing || deleted != JNothing) {
        println(k)
        println("")
      }
      k
    }

    s"parse correctly: '$inputStr'" in {
      s"original json must match expected one:\n $jsonDiff" ==> {
        json === jsonParsed
      }
    }
  }
}
