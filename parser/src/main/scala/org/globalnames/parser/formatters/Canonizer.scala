package org.globalnames
package parser
package formatters

import java.util.UUID

import scalaz.{Name => _, _}
import Scalaz._

case class Canonical(value: String) extends AnyVal {
  def id: UUID = UuidGenerator.generate(value)
}

trait Canonizer { parsedResult: ScientificNameParser.Result =>

  def canonizedUuid(showRanks: Boolean = false): Option[Canonical] =
    canonized(showRanks).map { Canonical }

  def canonized(showRanks: Boolean = false): Option[String] = {
    def canonizedNamesGroup(namesGroup: NamesGroup): String = {
      val name = namesGroup.name
      if (namesGroup.namedHybrid) {
        "× " + canonizedName(name, None)
      } else {
        val hybsCanonized = namesGroup.hybridParts.map {
          case (hc, Some(n)) =>
            val firstUni = namesEqual(namesGroup.name, n).option { namesGroup.name.uninomial }
            " " + canonizedName(n, firstUni)
          case (hc, None) => ""
        }
        (canonizedName(name, None) +: hybsCanonized).mkString(" ×")
      }
    }

    def canonizedName(nm: Name, firstName: Option[Uninomial]): String =
      Vector(firstName.map { fn => canonizedUninomial(fn, showRanks) }
                      .getOrElse(canonizedUninomial(nm.uninomial, showRanks)),
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
