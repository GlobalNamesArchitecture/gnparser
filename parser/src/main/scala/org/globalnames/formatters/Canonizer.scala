package org.globalnames.formatters

import org.globalnames.parser._
import scalaz.{Name => _, _}
import Scalaz._

trait Canonizer { parsedResult: ScientificNameParser.Result =>

  def canonized: Option[String] = {
    def canonizedNamesGroup(namesGroup: NamesGroup): Option[String] = namesGroup match {
      case NamesGroup(Seq(name), _, _) =>
        namesGroup.hybrid.map { _ => "× " } |+| canonizedName(name)
      case NamesGroup(names, _, _)     =>
        names.map(canonizedName).toVector.sequence.map { _.mkString(" × ") }
    }

    def canonizedName(nm: Name): Option[String] = {
      val parts =
        Vector(canonizedUninomial(nm.uninomial),
               nm.species.flatMap { canonizedSpecies },
               nm.infraspecies.flatMap { canonizedInfraspeciesGroup })
      if (parts.isEmpty) None
      else parts.flatten.mkString(" ").some
    }

    def canonizedSpecies(sp: Species): Option[String] =
      Util.norm(stringOf(sp)).some

    def canonizedInfraspecies(is: Infraspecies): Option[String] =
      Util.norm(stringOf(is)).some

    def canonizedInfraspeciesGroup(isg: InfraspeciesGroup): Option[String] =
      isg.group.map(canonizedInfraspecies).toVector.sequence.map { _.mkString(" ") }

    parsedResult.scientificName.namesGroup.flatMap(canonizedNamesGroup)
  }

  def canonizedUninomial(uninomial: Uninomial): Option[String] =
    (!uninomial.implied).option { Util.norm(stringOf(uninomial)) }
}
