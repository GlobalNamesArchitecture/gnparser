package org.globalnames.parser
package formatters

import org.json4s.JsonAST.{JArray, JNothing, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods

import scalaz.syntax.bind._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.std.option._

import spray.json._

class JsonRenderer(result: Result,
                   positionsRenderer: PositionsRenderer,
                   detailsRenderer: DetailsRenderer,
                   version: String) {

  import DetailsRendererJsonProtocol._

  private val canonicalOpt = result.canonical

  private def convert(inputJson: JsValue): JValue = {
    JsonMethods.parse(inputJson.compactPrint)
  }

  def json(showCanonicalUuid: Boolean = false): JValue = {
    val parsed = canonicalOpt.isDefined

    val canonicalName: JValue =
      if (parsed) {
        val canonizedUuidStrOpt = canonicalOpt.map { _.id.toString }
        ("id" -> showCanonicalUuid.option { canonizedUuidStrOpt }.join) ~
          ("value" -> canonicalOpt.map { _.value }) ~
          ("valueRanked" -> canonicalOpt.map { _.ranked })
      } else JNothing

    val quality = canonicalOpt.map { _ => result.scientificName.quality }
    val qualityWarnings: Option[JArray] =
      if (result.warnings.isEmpty) None
      else {
        val warningsJArr: JArray =
          result.warnings.sorted
            .map { w => JArray(List(w.level, w.message)) }.distinct
        warningsJArr.some
      }
    val positionsJson: Option[JArray] = parsed.option {
      positionsRenderer.positioned.map { position =>
        JArray(List(position.nodeName,
          result.preprocessorResult.verbatimPosAt(position.start),
          result.preprocessorResult.verbatimPosAt(position.end)))
      }
    }

    val details =
      detailsRenderer.details.isEmpty ?
        (JNothing: JValue) |
        convert(detailsRenderer.details.toJson)

    JsonMethods.render(
      ("nameStringId" -> result.preprocessorResult.id.toString) ~
      ("parsed" -> parsed) ~
      ("quality" -> quality) ~
      ("qualityWarnings" -> qualityWarnings) ~
      ("parserVersion" -> version) ~
      ("verbatim" -> result.preprocessorResult.verbatim) ~
      ("normalized" -> result.normalized) ~
      ("canonicalName" -> canonicalName) ~
      ("hybrid" -> result.scientificName.hybrid) ~
      ("surrogate" -> result.scientificName.surrogate) ~
      ("unparsedTail" -> result.scientificName.unparsedTail) ~
      ("virus" -> result.preprocessorResult.virus) ~
      ("bacteria" -> result.scientificName.bacteria) ~
      ("details" -> details) ~
      ("positions" -> positionsJson))
  }
}
