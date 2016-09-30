package org.globalnames.parser

import org.specs2.mutable.Specification

class DelimitedStringRendererSpec extends Specification {
  val scientificNameParser = new ScientificNameParser { val version = "test_version" }

  "correctly generate delimited string with explicit delimiter" in {
    scientificNameParser.fromString("Aaaba de Laubenfels, 1936").delimitedString("|") ===
      "abead069-293d-5299-badd-c10c0f5545fb|Aaaba de Laubenfels, 1936|" +
      "Aaaba|Aaaba|de Laubenfels 1936|1936|1"
  }
}
