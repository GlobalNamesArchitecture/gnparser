package org.globalnames.parser.formatters

import org.globalnames.parser._
import scalaz.{Name => _, _}
import Scalaz._

trait Normalizer { parsedResult: ScientificNameParser.Result =>

  def normalized: Option[String] = {

    def normalizedNamesGroup(namesGroup: NamesGroup): Option[String] = {
      val name = namesGroup.name
      if (namesGroup.namedHybrid) {
        "× ".some |+| normalizedName(name, None)
      } else {
        val hybsNormal = namesGroup.hybridParts.map {
          case (hc, Some(n)) =>
            normalizedName(n, namesEqual(name, n).option { name.uninomial }).map { " × " + _ }
          case (hc, None) => " ×".some
        }
        val parts = normalizedName(name, None) +: hybsNormal
        parts.reduce { _ |+| _ }
      }
    }

    def normalizedName(nm: Name, firstName: Option[Uninomial]): Option[String] = {
      val parts =
        Vector(firstName.map { normalizedUninomial }
                        .getOrElse(normalizedUninomial(nm.uninomial)),
               nm.subgenus.flatMap { normalizedSubGenus }.map { "(" + _ + ")" },
               nm.comparison.map { stringOf },
               nm.species.flatMap { normalizedSpecies },
               nm.infraspecies.flatMap { normalizedInfraspeciesGroup })
      parts.nonEmpty.option { parts.flatten.mkString(" ") }
    }

    def normalizedUninomial(u: Uninomial): Option[String] =
      (!u.implied).option {
        val parts =
          Vector(canonizedUninomial(u, showRanks = true),
                 u.authorship.flatMap { normalizedAuthorship })
        parts.flatten.mkString(" ")
      }

    def normalizedUninomialWord(uw: UninomialWord): Option[String] =
      stringOf(uw).some

    def normalizedSubGenus(sg: SubGenus): Option[String] =
      normalizedUninomialWord(sg.word)

    def normalizedSpecies(sp: Species): Option[String] = {
      Util.normalize(stringOf(sp)).some |+|
        sp.authorship.flatMap(normalizedAuthorship).map { " " + _ }
    }

    def normalizedInfraspecies(is: Infraspecies): Option[String] = {
      is.rank.map { r => r.typ.getOrElse(stringOf(r)) + " " } |+|
        Util.normalize(stringOf(is)).some |+|
        is.authorship.flatMap(normalizedAuthorship).map { " " + _ }
    }

    def normalizedInfraspeciesGroup(isg: InfraspeciesGroup): Option[String] =
      isg.group.map(normalizedInfraspecies).toVector.sequence.map { _.mkString(" ") }

    parsedResult.scientificName.namesGroup.flatMap { normalizedNamesGroup }
  }

  def normalizedYear(y: Year): String = {
    if (y.approximate) "(" + stringOf(y) + ")" else stringOf(y)
  }

  def normalizedAuthor(a: Author): String = {
    if (a.anon) "unknown"
    else {
      val authorStr = a.words.foldLeft(new StringBuilder) { (sb, aw) =>
        aw.separator match {
          case AuthorWordSeparator.None =>
          case AuthorWordSeparator.Dash => sb.append("-")
          case AuthorWordSeparator.Space => sb.append(" ")
        }
        sb.append(Util.normalizeAuthorWord(stringOf(aw)))
      }
      (authorStr.toString.some |+| a.filius.map { _ => " f." }).orZero
    }
  }

  def normalizeAuthorSeparator(as: AuthorSep, last: Boolean): String = {
    stringOf(as) match {
      case "," => last ? " & " | ", "
      case "&" | "and" | "et" => " & "
      case "apud" => " apud "
    }
  }

  def normalizedAuthorsTeam(at: AuthorsTeam): Option[String] =
    if (at.authors.size == 1) {
      normalizedAuthor(at.authors.head).some
    } else {
      val authsStr = at.authors.zipWithIndex.foldLeft(new StringBuilder) { case (sb, (a, idx)) =>
        if (idx > 0) {
          a.separator match {
            case Some(sep) =>
              val isLast = idx == at.authors.size - 1
              sb.append(normalizeAuthorSeparator(sep, isLast))
            case None =>
              sb.append(", ")
          }
        }
        sb.append(normalizedAuthor(a))
      }
      authsStr.toString.some
    }

  def normalizedAuthorsGroup(ag: AuthorsGroup): Option[String] = {
    normalizedAuthorsTeam(ag.authors) |+|
      ag.authorsEx.flatMap(normalizedAuthorsTeam).map { " ex " + _ } |+|
      ag.year.map(normalizedYear).map { " " + _ }
  }

  def normalizedAuthorship(as: Authorship): Option[String] = {
    normalizedAuthorsGroup(as.authors).map { x =>
      if (as.inparenthesis) "(" + x + ")" else x
    } |+| as.combination.flatMap(normalizedAuthorsGroup).map { " " + _ }
  }
}
