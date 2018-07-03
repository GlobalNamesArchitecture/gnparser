package org.globalnames.parser

import formatters._

import org.json4s.JValue

class ResultRendered(val result: Result, version: String) {
  val details: Details = new Details(result)
  val jsonRenderer: JsonRenderer = new JsonRenderer(result, version, details)
  val delimitedStringRenderer: DelimitedStringRenderer = new DelimitedStringRenderer(result)

  def json(showCanonicalUuid: Boolean = false): JValue =
    jsonRenderer.json(showCanonicalUuid)

  def delimitedString(delimiter: String = "\t"): String =
    delimitedStringRenderer.delimitedString(delimiter)
}

abstract class ScientificNameParser {
  val version: String

  def fromString(input: String): ResultRendered =
    fromString(input, collectParsingErrors = false)

  def fromString(input: String,
                 collectParsingErrors: Boolean): ResultRendered = {
    val result = Result.fromString(input, collectParsingErrors)
    new ResultRendered(result, version)
  }
}

object ScientificNameParser {

  final val instance = new ScientificNameParser {
    val version: String = BuildInfo.version
  }

}
