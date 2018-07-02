package org.globalnames.parser

import org.json4s._
import org.specs2.mutable.Specification

class ParserSpec extends Specification {

  val snp: ScientificNameParserRenderer = new ScientificNameParserRenderer {
    val parser: ScientificNameParser = new ScientificNameParser {
      override val version: String = "test_version"
    }
  }

  implicit val formats = DefaultFormats

  def parse(input: String): Result = {
    snp.fromString(input).result
  }

  "Parses:" >> {
    "Homo sapiens" in {
      val res = parse("Homo sapiens")
      res.canonizer.canonized().value === "Homo sapiens"
      res.warnings must beEmpty
    }

    """Homo\nsapiens""" in {
      val res = parse("Homo\nsapiens")

      res.warnings must haveSize(1)
      res.warnings(0).level === 3
      res.warnings(0).message === "Non-standard space characters"

      res.canonizer.canonized() === "Homo sapiens"
    }

    """Homo\r\nsapiens""" in {
      val res = parse("Homo\r\nsapiens")

      res.warnings must haveSize(2)
      res.warnings(0).level === 2
      res.warnings(0).message === "Multiple adjacent space characters"
      res.warnings(1).level === 3
      res.warnings(1).message === "Non-standard space characters"

      res.canonizer.canonized() === "Homo sapiens"
    }

    """Homo sapiens\r""" in {
      val res = parse("Homo sapiens\r")

      res.warnings must haveSize(1)
      res.warnings(0).level === 2
      res.warnings(0).message === "Trailing whitespace"

      res.canonizer.canonized() === "Homo sapiens"
    }

    """Homo sp.\r""" in {
      val res = parse("Homo sp.\r")

      res.warnings must haveSize(2)
      res.warnings(0).level === 2
      res.warnings(0).message === "Trailing whitespace"
      res.warnings(1).level === 3
      res.warnings(1).message === "Name is approximate"

      res.canonizer.canonized() === "Homo"
    }
  }

  "Does not parse:" >> {
    "whateva" in {
      val res = snp.fromString("whateva")
      (res.jsonRenderer.json() \\ "parsed").extract[Boolean] must beFalse
    }
  }
}
