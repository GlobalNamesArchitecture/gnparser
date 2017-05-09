package org.globalnames.parser.formatters

import org.globalnames.parser.ScientificNameParser
import scalaz._
import Scalaz._

trait DelimitedStringRenderer {
  parserResult: ScientificNameParser.Result with Normalizer =>

  protected[globalnames] val ambiguousAuthorship: Boolean = {
    val isAmbiguousOpt = for {
      hybrid <- parserResult.scientificName.hybrid
      ng <- parserResult.scientificName.namesGroup
    } yield hybrid && !ng.namedHybrid
    isAmbiguousOpt.getOrElse(false)
  }

  protected[globalnames] val authorshipDelimited: Option[String] =
    (!ambiguousAuthorship).option {
      parserResult.scientificName.authorship.flatMap { normalizedAuthorship }
    }.flatten

  val yearDelimited: Option[String] =
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
    val uuid = parserResult.preprocessorResult.id
    val verbatim = parserResult.preprocessorResult.verbatim
    val canonical = parserResult.canonized().orZero
    val canonicalExtended = parserResult.canonized(showRanks = true).orZero
    val quality = parserResult.scientificName.quality
    Seq(uuid, verbatim, canonical, canonicalExtended,
        authorshipDelimited.orZero, yearDelimited.orZero, quality).mkString(delimiter)
  }

  /**
    * List of authors of author words:
    * "Nothoprodontia boliviana MONNÉ Miguel Ángel, MONNÉ Marcela Laura, 2004" authorship names as
    * Seq(Seq(MONNÉ, Miguel, Ángel), Seq(MONNÉ, Marcela, Laura))
    */
  val authorshipNames: Seq[Seq[String]] = {
    if (ambiguousAuthorship) {
      Seq()
    } else {
      parserResult.scientificName.authorship.map { as =>
        val authorsNames = as.authors.authors.authors.map { a => a.words.map { w => stringOf(w) } }
        val authorsExNames = as.authors.authorsEx.map { at => at.authors.map { a =>
          a.words.map { w => stringOf(w) }
        }}.getOrElse(Seq())
        authorsNames ++ authorsExNames
      }.getOrElse(Seq())
    }
  }
}
