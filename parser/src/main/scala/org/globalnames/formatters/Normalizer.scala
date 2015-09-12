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
      normalizedUninomial(nm.uninomial) |+|
        nm.subgenus.flatMap(normalizedSubGenus).map(" (" + _ + ")") |+|
        nm.comparison.map { c => " " + input.substring(c.pos) } |+|
        nm.species.flatMap(normalizedSpecies).map(" " + _) |+|
        nm.infraspecies.flatMap(normalizedInfraspeciesGroup).map(" " + _)
    }

    def normalizedUninomial(u: Uninomial): Option[String] = {
      u.parent.map { canonizedUninomial(_) + " " } |+|
        u.rank.map { _.typ + " " } |+| canonizedUninomial(u).some |+|
        u.authorship.flatMap(normalizedAuthorship).map { " " + _ }
    }

    def normalizedUninomialWord(uw: UninomialWord): Option[String] =
      parsedResult.input.substring(uw.pos).some

    def normalizedSubGenus(sg: SubGenus): Option[String] = normalizedUninomialWord(sg.subgenus)

    def normalizedSpecies(sp: Species): Option[String] = {
      Util.norm(input.substring(sp.pos)).some |+|
        sp.authorship.flatMap(normalizedAuthorship).map(" " + _)
    }

    def normalizedInfraspecies(is: Infraspecies): Option[String] = {
      is.rank.map(_.typ + " ") |+|
        Util.norm(input.substring(is.pos)).some |+|
        is.authorship.flatMap(normalizedAuthorship).map(" " + _)
    }

    def normalizedInfraspeciesGroup(isg: InfraspeciesGroup): Option[String] =
      isg.group.map(normalizedInfraspecies).toVector.sequence.map { _.mkString(" ") }

    parsedResult.scientificName.namesGroup.flatMap { normalizedNamesGroup }
  }

  def normalizedYear(y: Year): String = {
    val yearStr = parsedResult.input.substring(y.digitsPos)
    if (y.approximate) "(" + yearStr + ")" else yearStr
  }

  def normalizedAuthor(a: Author): String = {
    if (a.anon) "unknown"
    else {
      val authorStr =
        a.words
          .map(p => Util.normAuthWord(parsedResult.input.substring(p)))
          .mkString(" ")
      (authorStr.some |+| a.filius.map(_ => " f.")).orZero
    }
  }

  def normalizedAuthorship(as: Authorship): Option[String] = {
    def normalizedAuthorsTeam(at: AuthorsTeam): Option[String] = at match {
      case AuthorsTeam(Vector(auth), _) => normalizedAuthor(auth).some
      case AuthorsTeam(auths, _) =>
        val authsStr = auths.dropRight(1).map(normalizedAuthor).mkString(", ") +
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
