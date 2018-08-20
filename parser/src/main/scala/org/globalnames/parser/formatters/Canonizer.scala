package org.globalnames
package parser
package formatters

import ast._

import java.util.UUID

import scalaz.std.option._
import scalaz.std.string._
import scalaz.syntax.bind._
import scalaz.syntax.semigroup._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._

trait Canonizer { self: Result with ResultOps =>

  import Canonizer.Canonical

  def canonical: Option[Canonical] = {
    for (canonical <- computeCanonical(showRanks = false)) yield {
      Canonical(value = canonical,
                ranked = computeCanonical(showRanks = true).orZero)
    }
  }

  private def computeCanonical(showRanks: Boolean): Option[String] = {
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
      Util.normalize(stringOf(sp))

    def canonizedInfraspecies(is: Infraspecies): String = {
      val rankStrMaybe = showRanks.option {
        is.rank.map { r => r.typ.getOrElse(stringOf(r)) + " " }}.join
      rankStrMaybe.orZero + Util.normalize(stringOf(is))
    }

    def canonizedInfraspeciesGroup(isg: InfraspeciesGroup): String =
      isg.group.map(canonizedInfraspecies).mkString(" ")

    self.scientificName.namesGroup.map(canonizedNamesGroup)
  }

  private[formatters] def canonizedUninomial(uninomial: Uninomial,
                                             showRanks: Boolean): Option[String] =
    (!uninomial.implied).option {
      val rankStrMaybe = showRanks.option {
        uninomial.parent.map { p => Util.normalize(stringOf(p)) + " " } |+|
        uninomial.rank.map { r => r.typ.getOrElse(stringOf(r)) }}.join
      rankStrMaybe.map { _ + " " }.orZero + Util.normalize(stringOf(uninomial))
    }
}

object Canonizer {

  case class Canonical(ranked: String, value: String) {
    val id: UUID = UuidGenerator.generate(value)
    val idRanked: UUID = UuidGenerator.generate(ranked)
  }

}
