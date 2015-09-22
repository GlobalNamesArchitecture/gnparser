package org.globalnames.formatters

import org.globalnames.parser._
import scalaz.{Name => _, _}
import Scalaz._

trait Normalizer { parsedResult: ScientificNameParser.Result
                     with Canonizer =>

  def normalized: Option[String] = {
    def normalizedNamesGroup(namesGroup: NamesGroup): Option[String] = {
      val name = namesGroup.name
      if (name.size == 1)
        namesGroup.hybrid.map { _ => "× " } |+| normalizedName(name.head)
      else
        name.map(normalizedName).toVector.sequence.map { _.mkString(" × ") }
    }

    def normalizedName(nm: Name): Option[String] = {
      val parts =
        Vector(normalizedUninomial(nm.uninomial),
               nm.subgenus.flatMap { normalizedSubGenus }.map { "(" + _ + ")" },
               nm.comparison.map { stringOf },
               nm.species.flatMap { normalizedSpecies },
               nm.infraspecies.flatMap { normalizedInfraspeciesGroup })
      if (parts.isEmpty) None
      else parts.flatten.mkString(" ").some
    }

    def normalizedUninomial(u: Uninomial): Option[String] =
      (!u.implied).option {
        val parts =
          Vector(u.parent.flatMap { canonizedUninomial },
            u.rank.map { r => r.typ.getOrElse(stringOf(r)) },
            canonizedUninomial(u),
            u.authorship.flatMap { normalizedAuthorship })
        parts.flatten.mkString(" ")
      }

    def normalizedUninomialWord(uw: UninomialWord): Option[String] =
      stringOf(uw).some

    def normalizedSubGenus(sg: SubGenus): Option[String] = normalizedUninomialWord(sg.subgenus)

    def normalizedSpecies(sp: Species): Option[String] = {
      Util.norm(stringOf(sp)).some |+|
        sp.authorship.flatMap(normalizedAuthorship).map(" " + _)
    }

    def normalizedInfraspecies(is: Infraspecies): Option[String] = {
      is.rank.map { r => r.typ.getOrElse(stringOf(r)) + " " } |+|
        Util.norm(stringOf(is)).some |+|
        is.authorship.flatMap(normalizedAuthorship).map(" " + _)
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
      val authorStr =
        a.words
          .map(p => Util.normAuthWord(stringOf(p)))
          .mkString(" ")
      (authorStr.some |+| a.filius.map(_ => " f.")).orZero
    }
  }

  def normalizedAuthorship(as: Authorship): Option[String] = {
    def normalizedAuthorsTeam(at: AuthorsTeam): Option[String] =
      if (at.authors.size == 1) {
        normalizedAuthor(at.authors.head).some
      } else {
        val auths = at.authors
        val authsStr = auths.dropRight(1).map{ normalizedAuthor }.mkString(", ") +
          " & " + normalizedAuthor(auths.last)
        authsStr.some
      }

    def normalizedAuthorsGroup(ag: AuthorsGroup): Option[String] = {
      normalizedAuthorsTeam(ag.authors) |+|
        ag.authorsEx.flatMap(normalizedAuthorsTeam).map(" ex " + _) |+|
        ag.year.map(normalizedYear).map(" " + _)
    }

    normalizedAuthorsGroup(as.authors).map { x => if (as.inparenthesis) "(" + x + ")" else x } |+|
      as.combination.flatMap(normalizedAuthorsGroup).map(" " + _)
  }
}
