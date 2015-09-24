package org.globalnames.formatters

import org.globalnames.parser._
import scalaz.{Name => _, _}
import Scalaz._

trait Canonizer { parsedResult: ScientificNameParser.Result =>

  def canonized(showRanks: Boolean): Option[String] = {
    def canonizedNamesGroup(namesGroup: NamesGroup): Option[String] =
      if (namesGroup.name.size == 1) {
        namesGroup.hybrid.map { _ => "× " } |+| canonizedName(namesGroup.name.head)
      } else {
        namesGroup.name.map(canonizedName).toVector.sequence.map { _.mkString(" × ") }
      }

    def canonizedName(nm: Name): Option[String] = {
      val parts =
        Vector(canonizedUninomial(nm.uninomial, showRanks),
               nm.species.flatMap { canonizedSpecies },
               nm.infraspecies.flatMap { canonizedInfraspeciesGroup })
      if (parts.isEmpty) None
      else parts.flatten.mkString(" ").some
    }

    def canonizedSpecies(sp: Species): Option[String] =
      Util.norm(stringOf(sp)).some

    def canonizedInfraspecies(is: Infraspecies): Option[String] = {
      is.rank.map { r => r.typ.getOrElse(stringOf(r)) }.map { _ + " " } |+|
        Util.norm(stringOf(is)).some
    }

    def canonizedInfraspeciesGroup(isg: InfraspeciesGroup): Option[String] =
      isg.group.map(canonizedInfraspecies).toVector.sequence.map { _.mkString(" ") }

    parsedResult.scientificName.namesGroup.flatMap(canonizedNamesGroup)
  }

  def canonizedUninomial(uninomial: Uninomial, showRanks: Boolean): Option[String] =
    (!uninomial.implied).option {
      Vector(showRanks.option {
               uninomial.rank.map { r => r.typ.getOrElse(stringOf(r)) }}.join,
             Util.norm(stringOf(uninomial)).some
            ).flatten.mkString(" ")
    }
}
