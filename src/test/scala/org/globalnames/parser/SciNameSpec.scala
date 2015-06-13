package org.globalnames.parser

import org.specs2.mutable.Specification

class SciNameSpec extends Specification {
  val gp = SciName("Betula")

  "GnParser" should {
    "initialize by string" in {
      gp must haveClass[SciName]
    }
    "have verbatim field" in {
      gp.verbatim == "Betula"
    }
    "field isParsed reassigns to true" in {
      gp.isParsed must beFalse
      gp.isParsed = true
      gp.isParsed must beTrue
    }
  }
}
