package org.globalnames.formatters

import org.globalnames.parser._
import scalaz.{Name => _, _}
import Scalaz._

object Canonizer {
  def format(scientificName: ScientificName): Option[String] = {
    def formatNamesGroup(namesGroup: NamesGroup): Option[String] = namesGroup match {
      case NamesGroup(Seq(name), _, _) =>
        namesGroup.hybrid.option("× ") |+| formatName(name)
      case NamesGroup(names, _, _)     =>
        names.map(formatName).toVector.sequence.map { _.mkString(" × ") }
    }

    def formatName(nm: Name): Option[String] = {
      formatUninomial(nm.uninomial) |+|
        nm.species.flatMap(formatSpecies).map(" " + _) |+|
        nm.infraspecies.flatMap(formatInfraspeciesGroup).map(" " + _)
    }

    def formatUninomial(u: Uninomial): Option[String] = Util.norm(u.str).some

    def formatSpecies(sp: Species): Option[String] = Util.norm(sp.str).some

    def formatInfraspecies(is: Infraspecies): Option[String] = Util.norm(is.str).some

    def formatInfraspeciesGroup(isg: InfraspeciesGroup): Option[String] =
      isg.group.map(formatInfraspecies).toVector.sequence.map { _.mkString(" ") }

    def formatYear(y: Year): Option[String] =
      scientificName.input.verbatim.substring(y.pos.start, y.pos.end - 1).some

    def formatAuthor(a: Author): Option[String] = a.str.some |+| a.filius.option(" f.")

    def formatAuthorsTeam(at: AuthorsTeam): Option[String] = at match {
      case AuthorsTeam(Vector(auth), _) => formatAuthor(auth)
      case AuthorsTeam(auths, _) =>
        auths.dropRight(1).map(formatAuthor).toVector.sequence.map { _.mkString(", ") } |+|
        formatAuthor(auths.last).map(" & " + _)
    }

    def formatAuthorsGroup(ag: AuthorsGroup): Option[String] = formatAuthorsTeam(ag.authors)

    def formatAuthorship(as: Authorship): Option[String] = {
      formatAuthorsGroup(as.authors).map { x => if (as.inparenthesis) "(" + x + ")" else x } |+|
        as.combination.flatMap(formatAuthorsGroup).map(" " + _)
    }

    scientificName.namesGroup.flatMap(formatNamesGroup)
  }
}
