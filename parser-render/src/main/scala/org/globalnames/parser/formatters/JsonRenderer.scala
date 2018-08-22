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
                   positions: Positions,
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
          ("value_ranked" -> canonicalOpt.map { _.ranked })
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
      positions.positioned.map { position =>
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
      ("name_string_id" -> result.preprocessorResult.id.toString) ~
      ("parsed" -> parsed) ~
      ("quality" -> quality) ~
      ("quality_warnings" -> qualityWarnings) ~
      ("parser_version" -> version) ~
      ("verbatim" -> result.preprocessorResult.verbatim) ~
      ("normalized" -> result.normalized) ~
      ("canonical_name" -> canonicalName) ~
      ("hybrid" -> result.scientificName.hybrid) ~
      ("surrogate" -> result.scientificName.surrogate) ~
      ("unparsed_tail" -> result.scientificName.unparsedTail) ~
      ("virus" -> result.preprocessorResult.virus) ~
      ("bacteria" -> result.scientificName.bacteria) ~
      ("details" -> details) ~
      ("positions" -> positionsJson))
  }
}
