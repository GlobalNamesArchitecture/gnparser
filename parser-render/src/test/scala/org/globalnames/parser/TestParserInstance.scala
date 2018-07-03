package org.globalnames.parser

trait TestParserInstance {

  val scientificNameParser: ScientificNameParserRenderer = new ScientificNameParserRenderer {
    val parser: ScientificNameParser = new ScientificNameParser {
      override val version: String = "test_version"
    }
  }

}
