package org.globalnames
package parser

import formatters._

import spray.json._

class RenderableResult(val result: Result, version: String) {
  private[parser] val delimitedStringRenderer = DelimitedStringRenderer(result)
  private[parser] val summarizer = new Summarizer(result, version)

  def summary(showCanonicalUuid: Boolean = false): Summarizer.Summary = {
    summarizer.summary(showCanonicalUuid)
  }

  def renderJsonString(compact: Boolean, showCanonicalUuid: Boolean): String = {
    import SummarizerProtocol.summaryFormat
    val jsonResult = summary(showCanonicalUuid).toJson
    if (compact) jsonResult.compactPrint
    else jsonResult.prettyPrint
  }

  def renderJsonString(compact: Boolean): String = {
    renderJsonString(compact, showCanonicalUuid = false)
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
