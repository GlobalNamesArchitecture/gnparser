package org.globalnames.parser.formatters

import org.globalnames.parser.ScientificNameParser
import scalaz._
import Scalaz._

trait DelimitedStringRenderer {
  parserResult: ScientificNameParser.Result with Normalizer =>

  protected[globalnames] val ambiguousAuthorship: Boolean = {
    parserResult.scientificName.isHybrid.fzip(parserResult.scientificName.namesGroup) match {
      case Some((true, ng)) => !(ng.hybridParts.size == 1 && ng.hybridParts.head._2.isEmpty)
      case _ => false
    }
  }

  protected[globalnames] val authorshipDelimited: Option[String] =
    (!ambiguousAuthorship).option {
      parserResult.scientificName.authorship.flatMap { normalizedAuthorship }
    }.flatten

  protected[globalnames] val yearDelimited: Option[String] =
    (!ambiguousAuthorship).option {
      parserResult.scientificName.year.map { normalizedYear }
    }.flatten

  /**
    * Renders selected fields of scientific name to delimiter-separated string.
    * Fields are: UUID, verbatim, canonical, canonical with ranks, last
    * significant authorship and year, and quality strings
    * @param delimiter delimits fields strings in result output. Default is TAB
    * @return fields concatenated to single string with delimiter
    */
  def delimitedString(delimiter: String = "\t"): String = {
    val uuid = parserResult.input.id
    val verbatim = parserResult.input.verbatim
    val canonical = parserResult.canonized().orZero
    val canonicalExtended = parserResult.canonized(showRanks = true).orZero
    val quality = parserResult.scientificName.quality
    Seq(uuid, verbatim, canonical, canonicalExtended,
        authorshipDelimited.orZero, yearDelimited.orZero, quality).mkString(delimiter)
  }
}
