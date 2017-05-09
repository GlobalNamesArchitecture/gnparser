package org.globalnames.parser.formatters

import org.globalnames.parser.ScientificNameParser
import org.json4s.JsonAST.{JArray, JNothing, JValue, JField, JObject}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods

import scalaz._
import Scalaz._

trait JsonRenderer { parserResult: ScientificNameParser.Result =>

  def json(showCanonicalUuid: Boolean = false): JValue = {
    val canonical = parserResult.canonized()
    val parsed = canonical.isDefined

    val canonicalName: JValue =
      if (parsed) {
        val minimal = {
          ("id" -> showCanonicalUuid.option { canonizedUuid().map { _.id.toString } }.join) ~
            ("value" -> canonical)
        }
        val canonicalExtended = parserResult.canonized(showRanks = true)
        if (canonical == canonicalExtended) minimal
        else minimal ~ ("extended" -> canonicalExtended)
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
      parserResult.positioned.map { position =>
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
      ("normalized" -> parserResult.normalized) ~
      ("canonical_name" -> canonicalName) ~
      ("hybrid" -> parserResult.scientificName.hybrid) ~
      ("surrogate" -> parserResult.scientificName.surrogate) ~
      ("unparsed_tail" -> parserResult.scientificName.unparsedTail) ~
      ("virus" -> parserResult.preprocessorResult.virus) ~
      ("bacteria" -> (parserResult.scientificName.bacteria ? true.some | none)) ~
      ("details" -> parserResult.detailed) ~
      ("positions" -> positionsJson))
  }

  def renderCompactJson: String = render(compact = true)

  def render(compact: Boolean, showCanonicalUuid: Boolean = false): String = {
    val jsonResult = json(showCanonicalUuid)
    if (compact) JsonMethods.compact(jsonResult)
    else JsonMethods.pretty(jsonResult)
  }
}
