package org.globalnames
package parser
package formatters

import scalaz.std.string._
import scalaz.std.option._
import scalaz.syntax.plus._
import scalaz.syntax.std.option._
import scalaz.syntax.std.boolean._
import Canonizer.Canonical

private[formatters] class DelimitedStringRenderer(result: Result) {

  private[parser] val ambiguousAuthorship: Boolean = {
    val isAmbiguousOpt = for {
      hybrid <- result.scientificName.hybrid
      ng <- result.scientificName.namesGroup
    } yield hybrid && !ng.namedHybrid
    isAmbiguousOpt.getOrElse(false)
  }

  private[parser] val authorshipDelimited: Option[String] =
    (!ambiguousAuthorship).option {
      result.scientificName.authorship.flatMap {
        result.normalizedAuthorship
      }
    }.flatten

  private[parser] def yearDelimited: Option[String] =
    (!ambiguousAuthorship).option {
      val year: Option[ast.Year] = result.scientificName.namesGroup.flatMap { ng =>
        val infraspeciesYear = ng.name.infraspecies.flatMap {
          _.group.last.authorship.flatMap { _.authors.authors.year }
        }
        val speciesYear =
          ng.name.species.flatMap { _.authorship.flatMap { _.authors.authors.year } }
        val uninomialYear = ng.name.uninomial.authorship.flatMap { _.authors.authors.year }
        infraspeciesYear <+> speciesYear <+> uninomialYear
      }
      year.map { result.normalizedYear }
    }.flatten

  /**
    * Renders selected fields of scientific name to delimiter-separated string.
    * Fields are: UUID, verbatim, canonical, canonical with ranks, last
    * significant authorship and year, and quality strings
    * @param delimiter delimits fields strings in result output. Default is TAB
    * @return fields concatenated to single string with delimiter
    */
  def delimitedString(delimiter: String = "\t"): String = {
    val uuid: String = result.preprocessorResult.id.toString
    val verbatim: String = result.preprocessorResult.verbatim
    val canonicalOpt: Option[Canonical] = result.canonical
    val canonicalStr: String = canonicalOpt.map { _.value }.orZero
    val canonicalRankedStr: String = canonicalOpt.map { _.ranked }.orZero
    val quality: Int = result.scientificName.quality
    val fields: Seq[String] = Seq(
      uuid, verbatim, canonicalStr, canonicalRankedStr,
      authorshipDelimited.orZero, yearDelimited.orZero,
      quality.toString)
    fields.mkString(delimiter)
  }

  /**
    * List of authors of author words:
    * "Nothoprodontia boliviana MONNÉ Miguel Ángel, MONNÉ Marcela Laura, 2004" authorship names as
    * Seq(Seq(MONNÉ, Miguel, Ángel), Seq(MONNÉ, Marcela, Laura))
    */
  private[parser] val authorshipNames: Seq[Seq[String]] = {
    if (ambiguousAuthorship) {
      Seq()
    } else {
      result.scientificName.authorship.map { as =>
        val authorsNames = as.authors.authors.authors.map {
          a => a.words.map { w => result.stringOf(w) }
        }
        val authorsExNames = as.authors.authorsEx.map { at => at.authors.map { a =>
          a.words.map { w => result.stringOf(w) }
        }}.getOrElse(Seq())
        val authorsEmendNames = as.authors.authorsEmend.map { at => at.authors.map { a =>
          a.words.map { w => result.stringOf(w) }
        }}.getOrElse(Seq())
        authorsNames ++ authorsExNames ++ authorsEmendNames
      }.getOrElse(Seq())
    }
  }
}

object DelimitedStringRenderer {

  def apply(result: Result): DelimitedStringRenderer = {
    new DelimitedStringRenderer(result)
  }

}
