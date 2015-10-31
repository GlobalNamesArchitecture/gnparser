package org.globalnames.formatters

import org.globalnames.parser.ScientificNameParser
import org.json4s.JsonAST.{JArray, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scalaz._
import Scalaz._

trait JsonRenderer { parserResult: ScientificNameParser.Result =>

  def json: JValue = {
    val canonical = parserResult.canonized(showRanks = false)
    val quality = canonical.map { _ => parserResult.scientificName.quality }
    val parsed = canonical.isDefined
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

    render("scientific_name" -> ("id" -> parserResult.input.id) ~
      ("parsed" -> parsed) ~
      ("quality" -> quality) ~
      ("quality_warnings" -> qualityWarnings) ~
      ("parser_version" -> version) ~
      ("verbatim" -> parserResult.input.verbatim) ~
      ("normalized" -> parserResult.normalized) ~
      ("canonical" -> canonical) ~
      ("canonical_extended" -> parserResult.canonized(showRanks = true)) ~
      ("hybrid" -> parserResult.scientificName.isHybrid) ~
      ("surrogate" -> parserResult.scientificName.surrogate) ~
      ("garbage" -> parserResult.scientificName.garbage) ~
      ("virus" -> parserResult.scientificName.isVirus) ~
      ("details" -> parserResult.detailed) ~
      ("positions" -> positionsJson))
  }

  def renderCompactJson: String = compact(json)
}
