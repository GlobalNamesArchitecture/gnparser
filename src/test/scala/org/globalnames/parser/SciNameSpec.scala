package org.globalnames.parser

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
    val data = line.split('|').map(_.trim).toList
    val parserVersion = """parser_version\":\"([^\"]*)\"""".r
    val parsed = SciName.fromString(data(0)).toJson
    val json = data(1).replaceAll(",.details.:.*", "}}")
    val res = parsed.replaceFirst("(parser_version.:.)[^\"]+", "$1test_version")
    s"SciName.fromString(${data(0)})" in {
      res === json
    }
  }
}
