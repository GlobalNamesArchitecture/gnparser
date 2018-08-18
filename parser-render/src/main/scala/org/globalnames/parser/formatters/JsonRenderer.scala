package org.globalnames.parser
package formatters

import org.json4s.JsonAST.{JArray, JNothing, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods

import scalaz._
import Scalaz._

class JsonRenderer(parserResult: Result, version: String, details: Details) {

  def json(showCanonicalUuid: Boolean = false): JValue = {
    val canonical = parserResult.canonical
    val parsed = canonical.isDefined

    val canonicalName: JValue =
      if (parsed) {
        val canonizedUuidStrOpt = parserResult.canonical.map { _.id.toString }
        ("id" -> showCanonicalUuid.option { canonizedUuidStrOpt }.join) ~
          ("value" -> canonical.map { _.value }) ~
          ("value_ranked" -> parserResult.canonical.map { _.ranked })
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
      parserResult.positions.positioned.map { position =>
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
      ("normalized" -> parserResult.normalizer.normalized) ~
      ("canonical_name" -> canonicalName) ~
      ("hybrid" -> parserResult.scientificName.hybrid) ~
      ("surrogate" -> parserResult.scientificName.surrogate) ~
      ("unparsed_tail" -> parserResult.scientificName.unparsedTail) ~
      ("virus" -> parserResult.preprocessorResult.virus) ~
      ("bacteria" -> parserResult.scientificName.bacteria) ~
      ("details" -> details.detailed) ~
      ("positions" -> positionsJson))
  }
}
