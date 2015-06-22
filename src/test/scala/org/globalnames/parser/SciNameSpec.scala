package org.globalnames.parser
import org.json4s._
import org.json4s.jackson._

import org.specs2.mutable.Specification
import org.specs2.mutable.Specification
import scala.io.Source
import scala.util.{Success, Failure}

class SciNameSpec extends Specification {
  val lines = Source.fromURL(
    getClass.getResource("/test_data.txt")).getLines

  def notComment(line: String): Boolean = {
    line.length > 0 && !("#\r\n\f\t " contains line.charAt(0))
  }

  for (line <- lines if notComment(line)) {
    val data = line.split('|').toList
    val parsed = SciName.fromString(data(0)).toJson
    val json = data(1).trim.replaceAll(""",\s*\"details.:.*""", "}}")
    val parserVersion = """parser_version\":\"([^\"]*)\"""".r
    val res = parsed.replaceFirst("(parser_version.:.)[^\"]+", "$1test_version")
    s"SciName.fromString(${data(0)})" in {
      JsonMethods.parse(res) === JsonMethods.parse(json)
    }
  }
}
