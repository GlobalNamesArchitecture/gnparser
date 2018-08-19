package org.globalnames.parser.examples

import org.globalnames.parser.ScientificNameParser.{instance => snp}

object ParserScala extends App {
  val jsonStr = snp.fromString("Homo sapiens L.").renderJson(compact = true)
  println(jsonStr)
}
