package org.globalnames
package parser

import formatters._

import org.json4s.JValue
import org.json4s.jackson.JsonMethods
import spray.json._

class RenderableResult(val result: Result, version: String) {
  private[parser] val detailsRenderer: DetailsRenderer = new DetailsRenderer(result)
  private[parser] val positionsRenderer: PositionsRenderer = new PositionsRenderer(result)
  private[parser] val delimitedStringRenderer: DelimitedStringRenderer =
    new DelimitedStringRenderer(result)
  private[parser] val jsonRenderer: JsonRenderer =
    new JsonRenderer(result, positionsRenderer, detailsRenderer, version)

  def json(showCanonicalUuid: Boolean): JValue = {
    import JsonRendererProtocol.summaryFormat
    JsonMethods.parse(jsonRenderer.json(showCanonicalUuid).toJson.compactPrint)
  }

  def json: JValue = {
    json(showCanonicalUuid = false)
  }

  def renderJson(compact: Boolean, showCanonicalUuid: Boolean): String = {
    val jsonResult = json(showCanonicalUuid)
    if (compact) JsonMethods.compact(jsonResult)
    else JsonMethods.pretty(jsonResult)
  }

  def renderJson(compact: Boolean): String = {
    renderJson(compact, showCanonicalUuid = false)
  }

  def renderDelimitedString(delimiter: String = "\t"): String = {
    delimitedStringRenderer.delimitedString(delimiter)
  }
}

abstract class ScientificNameParser {
  val version: String

  def fromString(input: String): RenderableResult = {
    fromString(input, collectParsingErrors = false)
  }

  def fromString(input: String,
                 collectParsingErrors: Boolean): RenderableResult = {
    val result = Result.fromString(input, collectParsingErrors)
    new RenderableResult(result, version)
  }
}

object ScientificNameParser {

  final val instance = new ScientificNameParser {
    val version: String = BuildInfo.version
  }

}
