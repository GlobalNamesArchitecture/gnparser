package org.globalnames.parser

trait TestParserInstance {

  val scientificNameParser: ScientificNameParser = new ScientificNameParser {
    override val version: String = "test_version"
  }

}
