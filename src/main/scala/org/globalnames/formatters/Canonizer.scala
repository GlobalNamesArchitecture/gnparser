package org.globalnames.formatters

import org.globalnames.parser._
import scalaz.{Name => _, _}
import Scalaz._

object Canonizer {
  def format(scientificName: ScientificName): Option[String] = scientificName match {
    case ScientificName(_, None, _) => None
    case ScientificName(_, Some(ng), _) => format(ng)
  }

  def format(namesGroup: NamesGroup): Option[String] = namesGroup match {
    case NamesGroup(Seq(name), _, _) => namesGroup.hybrid.option("× ") |+| format(name)
    case NamesGroup(names, _, _)     => names.map(format).toVector.sequence.map { _.mkString(" × ") }
  }

  def format(nm: Name): Option[String] = {
    format(nm.uninomial) |+|
      nm.species.flatMap(format).map(" " + _) |+|
      nm.infraspecies.flatMap(format).map(" " + _)
  }

  def format(u: Uninomial): Option[String] =Util.norm(u.str).some

  def format(sp: Species): Option[String] = Util.norm(sp.str).some

  def format(is: Infraspecies): Option[String] = Util.norm(is.str).some

  def format(isg: InfraspeciesGroup): Option[String] =
    isg.group.map(format).toVector.sequence.map { _.mkString(" ") }

  def format(y: Year): Option[String] = y.str.some

  def format(a: Author): Option[String] = a.str.some |+| a.filius.option(" f.")

  def format(at: AuthorsTeam): Option[String] = at match {
    case AuthorsTeam(Vector(auth), _) => format(auth)
    case AuthorsTeam(auths, _) => auths.dropRight(1).map(format).toVector.sequence.map { _.mkString(", ") } |+|
      format(auths.last).map(" & " + _)
  }

  def format(ag: AuthorsGroup): Option[String] = format(ag.authors)

  def format(as: Authorship): Option[String] = {
    format(as.authors).map { x => if (as.inparenthesis) "(" + x + ")" else x } |+|
      as.combination.flatMap(format).map(" " + _)
  }
}
