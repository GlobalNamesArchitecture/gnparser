package org.globalnames.parser
package formatters

import org.json4s.JsonAST.{JArray, JNothing, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods

import scalaz._
import Scalaz._

class JsonRenderer(parserResult: Result,
                   version: String,
                   canonizer: Canonizer,
                   normalizer: Normalizer,
                   positions: Positions,
                   details: Details) {

  def json(showCanonicalUuid: Boolean): JValue = {
    val canonical = canonizer.canonized()
    val parsed = canonical.isDefined

    val canonicalName: JValue =
      if (parsed) {
        val canonizedUuidStrOpt = canonizer.canonizedUuid().map { _.id.toString }
        ("id" -> showCanonicalUuid.option { canonizedUuidStrOpt }.join) ~
          ("value" -> canonical) ~
          ("value_ranked" -> canonizer.canonized(showRanks = true))
      } else JNothing

    val quality = canonical.map { _ => parserResult.scientificName.quality }
    val qualityWarnings: Option[JArray] =
      if (parserResult.warnings.isEmpty) None
      else {
        val warningsJArr: JArray =
          parserResult.warnings.sorted
            .map { w => JArray(List(w.level, w.message)) }.distinct
        warningsJArr.some
      }
    val positionsJson: Option[JArray] = parsed.option {
      positions.positioned.map { position =>
        JArray(List(position.nodeName,
          parserResult.preprocessorResult.verbatimPosAt(position.start),
          parserResult.preprocessorResult.verbatimPosAt(position.end)))
      }
    }

    JsonMethods.render(
      ("name_string_id" -> parserResult.preprocessorResult.id.toString) ~
      ("parsed" -> parsed) ~
      ("quality" -> quality) ~
      ("quality_warnings" -> qualityWarnings) ~
      ("parser_version" -> version) ~
      ("verbatim" -> parserResult.preprocessorResult.verbatim) ~
      ("normalized" -> normalizer.normalized) ~
      ("canonical_name" -> canonicalName) ~
      ("hybrid" -> parserResult.scientificName.hybrid) ~
      ("surrogate" -> parserResult.scientificName.surrogate) ~
      ("unparsed_tail" -> parserResult.scientificName.unparsedTail) ~
      ("virus" -> parserResult.preprocessorResult.virus) ~
      ("bacteria" -> parserResult.scientificName.bacteria) ~
      ("details" -> details.detailed) ~
      ("positions" -> positionsJson))
  }

  def renderCompactJson: String = render(compact = true)

  def render(compact: Boolean, showCanonicalUuid: Boolean = false): String = {
    val jsonResult = json(showCanonicalUuid)
    if (compact) JsonMethods.compact(jsonResult)
    else JsonMethods.pretty(jsonResult)
  }
}
