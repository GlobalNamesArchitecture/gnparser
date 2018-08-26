package org.globalnames.parser
package formatters

import formatters.{Summarizer => s}
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._

class Summarizer(result: Result, version: String) {

  private val canonicalOpt = result.canonical
  private val positionsGenerator: PositionsGenerator = new PositionsGenerator(result)
  private val detailsGenerator: DetailsGenerator = new DetailsGenerator(result)

  def summary(showCanonicalUuid: Boolean = false): s.Summary = {
    val parsed = canonicalOpt.isDefined

    val canonicalName = result.canonical.map { can =>
      s.CanonicalName(
        id = showCanonicalUuid.option { can.id.toString },
        value = can.value,
        valueRanked = can.ranked
      )
    }

    val quality = parsed.option { result.scientificName.quality }

    val qualityWarnings =
      (parsed && result.warnings.nonEmpty).option {
        result.warnings.map { w => (w.level, w.message) }
      }

    val positions: Option[Seq[s.PositionSummary]] = parsed.option {
      positionsGenerator.generate.map { position =>
        (position.nodeName,
          result.preprocessorResult.verbatimPosAt(position.start),
          result.preprocessorResult.verbatimPosAt(position.end))
      }
    }

    val detailsSummary = {
      val detailsOpt = parsed.option { detailsGenerator.generate }
      detailsOpt.flatMap { det => det.nonEmpty ? det.some | None }
    }

    s.Summary(
      nameStringId = result.preprocessorResult.id.toString,
      parsed = parsed,
      quality = quality,
      qualityWarnings = qualityWarnings,
      parserVersion = version,
      verbatim = result.preprocessorResult.verbatim,
      normalized = result.normalized,
      canonicalName = canonicalName,
      hybrid = result.scientificName.hybrid,
      surrogate = result.scientificName.surrogate,
      unparsedTail = result.scientificName.unparsedTail,
      virus = result.preprocessorResult.virus,
      bacteria = result.scientificName.bacteria,
      details = detailsSummary,
      positions = positions
    )
  }
}

object Summarizer {

  type PositionSummary = (String, Int, Int)
  type WarningSummary = (Int, String)

  case class Summary(nameStringId: String,
                     parsed: Boolean,
                     quality: Option[Int],
                     qualityWarnings: Option[Vector[(Int, String)]],
                     parserVersion: String,
                     verbatim: String,
                     normalized: Option[String],
                     canonicalName: Option[CanonicalName],
                     hybrid: Option[Boolean],
                     surrogate: Boolean,
                     unparsedTail: Option[String],
                     virus: Boolean,
                     bacteria: Boolean,
                     details: Option[Seq[DetailsGenerator.Name]],
                     positions: Option[Seq[(String, Int, Int)]])

  case class CanonicalName(id: Option[String],
                           value: String,
                           valueRanked: String)

}
