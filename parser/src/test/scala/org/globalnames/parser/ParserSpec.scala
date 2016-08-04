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
      res.warnings must beEmpty
    }

    """Homo\nsapiens""" in {
      val res = parse("Homo\nsapiens")

      res.warnings must haveSize(1)
      res.warnings(0).level === 3
      res.warnings(0).message === "Non-standard space characters"

      canonical(res.json()).extract[String] === "Homo sapiens"
    }

    """Homo\r\nsapiens""" in {
      val res = parse("Homo\r\nsapiens")

      res.warnings must haveSize(2)
      res.warnings(0).level === 2
      res.warnings(0).message === "Multiple adjacent space characters"
      res.warnings(1).level === 3
      res.warnings(1).message === "Non-standard space characters"

      canonical(res.json()).extract[String] === "Homo sapiens"
    }

    """Homo sapiens\r""" in {
      val res = parse("Homo sapiens\r")

      res.warnings must haveSize(1)
      res.warnings(0).level === 2
      res.warnings(0).message === "Trailing whitespace"

      canonical(res.json()).extract[String] === "Homo sapiens"
    }

    """Homo sp.\r""" in {
      val res = parse("Homo sp.\r")

      res.warnings must haveSize(2)
      res.warnings(0).level === 2
      res.warnings(0).message === "Trailing whitespace"
      res.warnings(1).level === 3
      res.warnings(1).message === "Name is approximate"

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
