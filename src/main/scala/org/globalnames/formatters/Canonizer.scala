package org.globalnames.formatters

import org.globalnames.parser._
import scalaz.{Name => _, _}
import Scalaz._

trait Canonizer { parsedResult: ScientificNameParser.Result =>

  def canonized: Option[String] = {
    def canonizedNamesGroup(namesGroup: NamesGroup): Option[String] = namesGroup match {
      case NamesGroup(Seq(name), _, _) =>
        namesGroup.hybrid.option("× ") |+| canonizedName(name)
      case NamesGroup(names, _, _)     =>
        names.map(canonizedName).toVector.sequence.map { _.mkString(" × ") }
    }

    def canonizedName(nm: Name): Option[String] = {
      canonizedUninomial(nm.uninomial) |+|
        nm.species.flatMap(canonizedSpecies).map(" " + _) |+|
        nm.infraspecies.flatMap(canonizedInfraspeciesGroup).map(" " + _)
    }

    def canonizedUninomial(u: Uninomial): Option[String] = Util.norm(u.str).some

    def canonizedSpecies(sp: Species): Option[String] = Util.norm(sp.str).some

    def canonizedInfraspecies(is: Infraspecies): Option[String] = Util.norm(is.str).some

    def canonizedInfraspeciesGroup(isg: InfraspeciesGroup): Option[String] =
      isg.group.map(canonizedInfraspecies).toVector.sequence.map { _.mkString(" ") }

    parsedResult.scientificName.namesGroup.flatMap(canonizedNamesGroup)
  }
}
