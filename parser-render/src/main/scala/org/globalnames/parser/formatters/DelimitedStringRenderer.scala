package org.globalnames
package parser
package formatters

import scalaz._
import Scalaz._

class DelimitedStringRenderer(parserResult: Result) extends CommonOps {
  protected val unescapedInput: String = parserResult.preprocessorResult.unescaped

  protected[globalnames] val ambiguousAuthorship: Boolean = {
    val isAmbiguousOpt = for {
      hybrid <- parserResult.scientificName.hybrid
      ng <- parserResult.scientificName.namesGroup
    } yield hybrid && !ng.namedHybrid
    isAmbiguousOpt.getOrElse(false)
  }

  protected[globalnames] val authorshipDelimited: Option[String] =
    (!ambiguousAuthorship).option {
      parserResult.scientificName.authorship.flatMap {
        parserResult.normalizer.normalizedAuthorship
      }
    }.flatten

  val yearDelimited: Option[String] =
    (!ambiguousAuthorship).option {
      val year: Option[Year] = parserResult.scientificName.namesGroup.flatMap { ng =>
        val infraspeciesYear = ng.name.infraspecies.flatMap {
          _.group.last.authorship.flatMap { _.authors.authors.year }
        }
        val speciesYear =
          ng.name.species.flatMap { _.authorship.flatMap { _.authors.authors.year } }
        val uninomialYear = ng.name.uninomial.authorship.flatMap { _.authors.authors.year }
        infraspeciesYear <+> speciesYear <+> uninomialYear
      }
      year.map { parserResult.normalizer.normalizedYear }
    }.flatten

  /**
    * Renders selected fields of scientific name to delimiter-separated string.
    * Fields are: UUID, verbatim, canonical, canonical with ranks, last
    * significant authorship and year, and quality strings
    * @param delimiter delimits fields strings in result output. Default is TAB
    * @return fields concatenated to single string with delimiter
    */
  def delimitedString(delimiter: String = "\t"): String = {
    val uuid: String = parserResult.preprocessorResult.id.toString
    val verbatim: String = parserResult.preprocessorResult.verbatim
    val canonical: String = parserResult.canonical.map { _.value }.orZero
    val canonicalExtended: String = parserResult.canonical.map { _.ranked }.orZero
    val quality: Int = parserResult.scientificName.quality
    val fields: Seq[String] = Seq(
      uuid, verbatim, canonical, canonicalExtended,
      authorshipDelimited.orZero, yearDelimited.orZero,
      quality.toString)
    fields.mkString(delimiter)
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
        val authorsNames = as.authors.authors.authors.map {
          a => a.words.map { w => stringOf(w) }
        }
        val authorsExNames = as.authors.authorsEx.map { at => at.authors.map { a =>
          a.words.map { w => stringOf(w) }
        }}.getOrElse(Seq())
        val authorsEmendNames = as.authors.authorsEmend.map { at => at.authors.map { a =>
          a.words.map { w => stringOf(w) }
        }}.getOrElse(Seq())
        authorsNames ++ authorsExNames ++ authorsEmendNames
      }.getOrElse(Seq())
    }
  }
}
