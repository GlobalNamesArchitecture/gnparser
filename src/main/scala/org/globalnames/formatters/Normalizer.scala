package org.globalnames.formatters

import org.globalnames.parser._
import scalaz.{Name => _, _}
import Scalaz._

trait Normalizer { parsedResult: ScientificNameParser.Result =>

  def normalized: Option[String] = {
    def normalizedNamesGroup(namesGroup: NamesGroup): Option[String] = {
      val name = namesGroup.name
      if (name.size == 1)
        namesGroup.hybrid.option("× ") |+| normalizedName(name.head)
      else
        name.map(normalizedName).toVector.sequence.map { _.mkString(" × ") }
    }

    def normalizedName(nm: Name): Option[String] = {
      normalizedUninomial(nm.uninomial) |+|
        nm.subgenus.flatMap(normalizedSubGenus).map(" (" + _ + ")") |+|
        nm.comparison.map(" " + _) |+|
        nm.species.flatMap(normalizedSpecies).map(" " + _) |+|
        nm.infraspecies.flatMap(normalizedInfraspeciesGroup).map(" " + _)
    }

    def normalizedUninomial(u: Uninomial): Option[String] =
      Util.norm(u.str).some |+| u.authorship.flatMap(normalizedAuthorship).map(" " + _)

    def normalizedUninomialWord(uw: UninomialWord): Option[String] = uw.str.some

    def normalizedSubGenus(sg: SubGenus): Option[String] = normalizedUninomialWord(sg.subgenus)

    def normalizedSpecies(sp: Species): Option[String] =
      Util.norm(sp.str).some |+| sp.authorship.flatMap(normalizedAuthorship).map(" " + _)

    def normalizedInfraspecies(is: Infraspecies): Option[String] = {
      is.rank.map(_ + " ") |+|
        Util.norm(is.str).some |+|
        is.authorship.flatMap(normalizedAuthorship).map(" " + _)
    }

    def normalizedInfraspeciesGroup(isg: InfraspeciesGroup): Option[String] =
      isg.group.map(normalizedInfraspecies).toVector.sequence.map { _.mkString(" ") }

    parsedResult.scientificName.namesGroup.flatMap { normalizedNamesGroup }
  }

  def normalizedYear(y: Year): Option[String] =
    parsedResult.input.unescaped.substring(y.pos.start, y.pos.end).some

  def normalizedAuthorship(as: Authorship): Option[String] = {
    def normalizedAuthor(a: Author): Option[String] = a.str.some |+| a.filius.option(" f.")

    def normalizedAuthorsTeam(at: AuthorsTeam): Option[String] = at match {
      case AuthorsTeam(Vector(auth), _) => normalizedAuthor(auth)
      case AuthorsTeam(auths, _) =>
        auths.dropRight(1).map(normalizedAuthor).toVector.sequence.map { _.mkString(", ") } |+|
        normalizedAuthor(auths.last).map(" & " + _)
    }

    def normalizedAuthorsGroup(ag: AuthorsGroup): Option[String] = {
      normalizedAuthorsTeam(ag.authors) |+|
        ag.authorsEx.flatMap(normalizedAuthorsTeam).map(" ex " + _) |+|
        ag.year.flatMap(normalizedYear).map(" " + _)
    }

    normalizedAuthorsGroup(as.authors).map { x => if (as.inparenthesis) "(" + x + ")" else x } |+|
      as.combination.flatMap(normalizedAuthorsGroup).map(" " + _)
  }
}
