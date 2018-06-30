package org.globalnames.parser

import org.json4s._
import org.specs2.mutable.Specification

class ParserSpec extends Specification {

  val snp = new ScientificNameParser {
    val version = "test_version"
  }
  import snp.{fromString => parse}

  implicit val formats = DefaultFormats

  "Parses:" >> {
    def canonical(json: JValue) = json \\ "canonical_name" \ "value"

    "Homo sapiens" in {
      val res = parse("Homo sapiens")
      canonical(res.json()).extract[String] === "Homo sapiens"
      res.result.warnings must beEmpty
    }

    """Homo\nsapiens""" in {
      val res = parse("Homo\nsapiens")

      res.result.warnings must haveSize(1)
      res.result.warnings(0).level === 3
      res.result.warnings(0).message === "Non-standard space characters"

      canonical(res.json()).extract[String] === "Homo sapiens"
    }

    """Homo\r\nsapiens""" in {
      val res = parse("Homo\r\nsapiens")

      res.result.warnings must haveSize(2)
      res.result.warnings(0).level === 2
      res.result.warnings(0).message === "Multiple adjacent space characters"
      res.result.warnings(1).level === 3
      res.result.warnings(1).message === "Non-standard space characters"

      canonical(res.json()).extract[String] === "Homo sapiens"
    }

    """Homo sapiens\r""" in {
      val res = parse("Homo sapiens\r")

      res.result.warnings must haveSize(1)
      res.result.warnings(0).level === 2
      res.result.warnings(0).message === "Trailing whitespace"

      canonical(res.json()).extract[String] === "Homo sapiens"
    }

    """Homo sp.\r""" in {
      val res = parse("Homo sp.\r")

      res.result.warnings must haveSize(2)
      res.result.warnings(0).level === 2
      res.result.warnings(0).message === "Trailing whitespace"
      res.result.warnings(1).level === 3
      res.result.warnings(1).message === "Name is approximate"

      canonical(res.json()).extract[String] === "Homo"
    }
  }

  "Does not parse:" >> {
    "whateva" in {
      val res = parse("whateva")
      (res.json() \\ "parsed").extract[Boolean] must beFalse
    }
  }
}
