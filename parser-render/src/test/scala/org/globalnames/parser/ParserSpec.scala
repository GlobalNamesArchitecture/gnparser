package org.globalnames.parser

import org.json4s._
import org.specs2.mutable.Specification

class ParserSpec extends Specification with TestParserInstance {

  implicit val formats = DefaultFormats

  def parse(input: String): Result = {
     scientificNameParser.fromString(input).result
  }

  "Parses:" >> {
    "Homo sapiens" in {
      val res = parse("Homo sapiens")
      res.canonical.map { _.value } should beSome(===("Homo sapiens"))
      res.warnings must beEmpty
    }

    """Homo\nsapiens""" in {
      val res = parse("Homo\nsapiens")

      res.warnings must haveSize(1)
      res.warnings(0).level === 3
      res.warnings(0).message === "Non-standard space characters"

      res.canonical.map { _.value } should beSome(===("Homo sapiens"))
    }

    """Homo\r\nsapiens""" in {
      val res = parse("Homo\r\nsapiens")

      res.warnings must haveSize(2)
      res.warnings(0).level === 2
      res.warnings(0).message === "Multiple adjacent space characters"
      res.warnings(1).level === 3
      res.warnings(1).message === "Non-standard space characters"

      res.canonical.map { _.value } should beSome(===("Homo sapiens"))
    }

    """Homo sapiens\r""" in {
      val res = parse("Homo sapiens\r")

      res.warnings must haveSize(1)
      res.warnings(0).level === 2
      res.warnings(0).message === "Trailing whitespace"

      res.canonical.map { _.value } should beSome(===("Homo sapiens"))
    }

    """Homo sp.\r""" in {
      val res = parse("Homo sp.\r")

      res.warnings must haveSize(2)
      res.warnings(0).level === 2
      res.warnings(0).message === "Trailing whitespace"
      res.warnings(1).level === 3
      res.warnings(1).message === "Name is approximate"

      res.canonical.map { _.value } should beSome(===("Homo"))
    }
  }

  "Does not parse:" >> {
    "whateva" in {
      val res = scientificNameParser.fromString("whateva")
      (res.json \\ "parsed").extract[Boolean] must beFalse
    }
  }
}
