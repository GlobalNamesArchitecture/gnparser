package org.globalnames.formatters

import org.globalnames.parser._
import scalaz.{Name => _, _}
import Scalaz._

object Normalizer {
  def format(scientificName: ScientificName): Option[String] = {
    def formatNamesGroup(namesGroup: NamesGroup): Option[String] = {
      val name = namesGroup.name
      if (name.size == 1)
        namesGroup.hybrid.option("× ") |+| formatName(name.head)
      else
        name.map(formatName).toVector.sequence.map { _.mkString(" × ") }
    }

    def formatName(nm: Name): Option[String] = {
      formatUninomial(nm.uninomial) |+|
        nm.subgenus.flatMap(formatSubGenus).map(" (" + _ + ")") |+|
        nm.comparison.map(" " + _) |+|
        nm.species.flatMap(formatSpecies).map(" " + _) |+|
        nm.infraspecies.flatMap(formatInfraspeciesGroup).map(" " + _)
    }

    def formatUninomial(u: Uninomial): Option[String] =
      Util.norm(u.str).some |+| u.authorship.flatMap(formatAuthorship(scientificName)).map(" " + _)

    def formatUninomialWord(uw: UninomialWord): Option[String] = uw.str.some

    def formatSubGenus(sg: SubGenus): Option[String] = formatUninomialWord(sg.subgenus)

    def formatSpecies(sp: Species): Option[String] =
      Util.norm(sp.str).some |+| sp.authorship.flatMap(formatAuthorship(scientificName)).map(" " + _)

    def formatInfraspecies(is: Infraspecies): Option[String] = {
      is.rank.map(_ + " ") |+|
        Util.norm(is.str).some |+|
        is.authorship.flatMap(formatAuthorship(scientificName)).map(" " + _)
    }

    def formatInfraspeciesGroup(isg: InfraspeciesGroup): Option[String] =
      isg.group.map(formatInfraspecies).toVector.sequence.map { _.mkString(" ") }

    scientificName.namesGroup.flatMap { formatNamesGroup }
  }

  def formatYear(scientificName: ScientificName)(y: Year): Option[String] =
    scientificName.input.unescaped.substring(y.pos.start, y.pos.end).some

  def formatAuthorship(scientificName: ScientificName)(as: Authorship): Option[String] = {
    def formatAuthor(a: Author): Option[String] = a.str.some |+| a.filius.option(" f.")

    def formatAuthorsTeam(at: AuthorsTeam): Option[String] = at match {
      case AuthorsTeam(Vector(auth), _) => formatAuthor(auth)
      case AuthorsTeam(auths, _) =>
        auths.dropRight(1).map(formatAuthor).toVector.sequence.map { _.mkString(", ") } |+|
        formatAuthor(auths.last).map(" & " + _)
    }

    def formatAuthorsGroup(ag: AuthorsGroup): Option[String] = {
      formatAuthorsTeam(ag.authors) |+|
        ag.authorsEx.flatMap(formatAuthorsTeam).map(" ex " + _) |+|
        ag.year.flatMap(formatYear(scientificName)).map(" " + _)
    }

    formatAuthorsGroup(as.authors).map { x => if (as.inparenthesis) "(" + x + ")" else x } |+|
      as.combination.flatMap(formatAuthorsGroup).map(" " + _)
  }
}
