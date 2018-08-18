package org.globalnames.parser

import formatters._

import org.json4s.JValue
import org.json4s.jackson.JsonMethods

class ResultRendered(val result: Result, version: String) {
  private val details: Details = new Details(result)
  private val delimitedStringRenderer: DelimitedStringRenderer = new DelimitedStringRenderer(result)

  def json(showCanonicalUuid: Boolean): JValue = {
    val jsonRenderer: JsonRenderer = new JsonRenderer(result, version, details)
    jsonRenderer.json(showCanonicalUuid)
  }

  def renderJson(compact: Boolean, showCanonicalUuid: Boolean): String = {
    val jsonResult = json(showCanonicalUuid)
    if (compact) JsonMethods.compact(jsonResult)
    else JsonMethods.pretty(jsonResult)
  }

  def renderJson(compact: Boolean): String =
    renderJson(compact, showCanonicalUuid = false)

  def renderDelimitedString(delimiter: String = "\t"): String = {
    delimitedStringRenderer.delimitedString(delimiter)
  }
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
