package org.globalnames.parser

import formatters._

import org.json4s.JValue

class ResultRendered(val result: Result) {
  val details: Details = new Details(result)
  val jsonRenderer: JsonRenderer = new JsonRenderer(result, details)
  val delimitedStringRenderer: DelimitedStringRenderer = new DelimitedStringRenderer(result)

  def json(showCanonicalUuid: Boolean = false): JValue =
    jsonRenderer.json(showCanonicalUuid)

  def delimitedString(delimiter: String = "\t"): String =
    delimitedStringRenderer.delimitedString(delimiter)
}

object ResultRendered {
  def apply(result: Result): ResultRendered = new ResultRendered(result)
}

abstract class ScientificNameParserRenderer {
  val parser: ScientificNameParser

  def fromString(input: String): ResultRendered =
    fromString(input, collectParsingErrors = false)

  def fromString(input: String,
                 collectParsingErrors: Boolean): ResultRendered = {
    val result = parser.fromString(input, collectParsingErrors)
    ResultRendered(result)
  }
}

object ScientificNameParserRenderer {

  final val instance = new ScientificNameParserRenderer {
    override final val parser: ScientificNameParser = ScientificNameParser.instance
  }

}
