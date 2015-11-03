package org.globalnames.formatters

import org.globalnames.parser.ScientificNameParser
import org.json4s.JsonAST.{JArray, JNothing, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scalaz._
import Scalaz._

trait JsonRenderer { parserResult: ScientificNameParser.Result =>

  def json: JValue = {
    val canonical = parserResult.canonized(showRanks = false)
    val parsed = canonical.isDefined

    val canonicalName: JValue =
      if (parsed) {
        val minimal = "value" -> canonical
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
          parserResult.input.verbatimPosAt(position.start),
          parserResult.input.verbatimPosAt(position.end)))
      }
    }

    render(
      ("name_string_id" -> parserResult.input.id) ~
      ("parsed" -> parsed) ~
      ("quality" -> quality) ~
      ("quality_warnings" -> qualityWarnings) ~
      ("parser_version" -> version) ~
      ("verbatim" -> parserResult.input.verbatim) ~
      ("normalized" -> parserResult.normalized) ~
      ("canonical_name" -> canonicalName) ~
      ("hybrid" -> parserResult.scientificName.isHybrid) ~
      ("surrogate" -> parserResult.scientificName.surrogate) ~
      ("garbage" -> parserResult.scientificName.garbage) ~
      ("virus" -> parserResult.scientificName.isVirus) ~
      ("details" -> parserResult.detailed) ~
      ("positions" -> positionsJson))
  }

  def renderCompactJson: String = compact(json)
}
