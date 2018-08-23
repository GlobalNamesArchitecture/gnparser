package org.globalnames.parser
package formatters

import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._

import spray.json._

import formatters.{JsonRenderer => jr}

object JsonRendererProtocol extends DefaultJsonProtocol {

  import DetailsRendererJsonProtocol.nameFormat

  implicit val canonicalNameFormat = jsonFormat3(jr.CanonicalName)

  implicit val summaryFormat = jsonFormat15(jr.Summary)

}

class JsonRenderer(result: Result,
                   positionsRenderer: PositionsRenderer,
                   detailsRenderer: DetailsRenderer,
                   version: String) {

  private val canonicalOpt = result.canonical

  def json(showCanonicalUuid: Boolean = false): jr.Summary = {
    val parsed = canonicalOpt.isDefined

    val canonicalName = result.canonical.map { can =>
      jr.CanonicalName(
        id = showCanonicalUuid.option { can.id.toString },
        value = can.value,
        valueRanked = can.ranked
      )
    }

    val quality = parsed.option { result.scientificName.quality }

    val qualityWarnings =
      (!parsed || result.warnings.isEmpty) ? Option.empty[Vector[jr.WarningSummary]] |
        result.warnings.sorted
              .map { w => (w.level, w.message) }
              .distinct
              .some

    val positions: Option[Seq[jr.PositionSummary]] = parsed.option {
      positionsRenderer.positioned.map { position =>
        (position.nodeName,
         result.preprocessorResult.verbatimPosAt(position.start),
         result.preprocessorResult.verbatimPosAt(position.end))
      }
    }

    val detailsSummary = {
      val detailsOpt = parsed.option { detailsRenderer.details }
      detailsOpt.flatMap { det => det.nonEmpty ? det.some | None }
    }

    jr.Summary(
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

object JsonRenderer {
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
                     details: Option[Seq[DetailsRenderer.Name]],
                     positions: Option[Seq[(String, Int, Int)]])

  case class CanonicalName(id: Option[String],
                           value: String,
                           valueRanked: String)
}
