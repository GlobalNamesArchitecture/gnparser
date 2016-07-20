package org.globalnames.formatters

import org.globalnames.parser._

import scalaz.{Name => _, _}
import Scalaz._

case class Canonical(value: String) extends AnyVal {
  def id = ScientificNameParser.uuidGenerator.generate(value)
}

trait Canonizer { parsedResult: ScientificNameParser.Result =>

  def canonizedUuid(showRanks: Boolean = false) = canonized(showRanks).map { Canonical }

  def canonized(showRanks: Boolean = false): Option[String] = {
    def canonizedNamesGroup(namesGroup: NamesGroup): String =
      if (namesGroup.name.size == 1) {
        namesGroup.hybrid.map { _ ⇒ "× " }.orZero +
          canonizedName(namesGroup.name.head)
      } else {
        namesGroup.name.map(canonizedName).mkString(" × ")
      }

    def canonizedName(nm: Name): String =
      Vector(canonizedUninomial(nm.uninomial, showRanks),
               nm.species.map { canonizedSpecies },
               nm.infraspecies.map { canonizedInfraspeciesGroup })
        .flatten.mkString(" ")

    def canonizedSpecies(sp: Species): String =
      Util.norm(stringOf(sp))

    def canonizedInfraspecies(is: Infraspecies): String = {
      val rankStrMaybe = showRanks.option {
        is.rank.map { r => r.typ.getOrElse(stringOf(r)) + " " }}.join
      rankStrMaybe.orZero + Util.norm(stringOf(is))
    }

    def canonizedInfraspeciesGroup(isg: InfraspeciesGroup): String =
      isg.group.map(canonizedInfraspecies).mkString(" ")

    parsedResult.scientificName.namesGroup.map(canonizedNamesGroup)
  }

  def canonizedUninomial(uninomial: Uninomial,
                         showRanks: Boolean): Option[String] =
    (!uninomial.implied).option {
      val rankStrMaybe = showRanks.option {
        uninomial.parent.map { p => Util.norm(stringOf(p)) + " " } |+|
        uninomial.rank.map { r => r.typ.getOrElse(stringOf(r)) }}.join
      rankStrMaybe.map { _ + " " }.orZero + Util.norm(stringOf(uninomial))
    }
}
