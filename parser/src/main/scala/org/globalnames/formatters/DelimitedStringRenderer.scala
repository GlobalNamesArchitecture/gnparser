package org.globalnames.formatters

import org.globalnames.parser.ScientificNameParser
import shapeless._
import scalaz._
import Scalaz._

trait DelimitedStringRenderer {
  parserResult: ScientificNameParser.Result with Normalizer =>

  protected[globalnames] val authorshipDelimited =
    parserResult.scientificName.authorship
                .flatMap { normalizedAuthorship }.orZero
  protected[globalnames] val yearDelimited =
    parserResult.scientificName.year
                .map { normalizedYear }.orZero

  /**
    * Renders selected fields of scientific name to delimiter-separated string.
    * Fields are: UUID, verbatim, canonical, canonical with ranks, last
    * significant authorship and year, and quality strings
    * @param delimiter delimits fields strings in result output. Default is TAB
    * @return fields concatenated to single string with delimiter
    */
  def delimitedString(delimiter: Char = '\t'): String = {
    val uuid = parserResult.input.id
    val verbatim = parserResult.input.verbatim
    val canonical = parserResult.canonized().orZero
    val canonicalExtended = parserResult.canonized(showRanks = true).orZero
    val quality = parserResult.scientificName.quality
    val result = uuid :: verbatim :: canonical :: canonicalExtended ::
      authorshipDelimited :: yearDelimited :: quality :: HNil
    result.mkString("", delimiter.toString, "")
  }
}
