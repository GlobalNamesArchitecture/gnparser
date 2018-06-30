package org.globalnames.parser.examples

import org.globalnames.parser.ScientificNameParser.{instance => snp}

object ParserScala extends App {
  val jsonStr = snp.fromString("Homo sapiens L.").jsonRenderer.renderCompactJson
  println(jsonStr)
}
