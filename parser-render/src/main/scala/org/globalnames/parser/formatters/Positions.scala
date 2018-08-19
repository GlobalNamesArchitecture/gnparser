package org.globalnames
package parser
package formatters

import scalaz.syntax.traverse._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.std.option._
import scalaz.std.vector._

class Positions(result: Result) {

  import Positions.Position

  def positioned: Seq[Position] = {
    def positionedNamesGroup(namesGroup: NamesGroup): Vector[Position] = {
      val (hchars, names) = namesGroup.hybridParts.unzip
      val namesPositions = names.flatMap { _.map { positionedName } }.flatten
      val hcharsPositions = hchars.map { positionedHybridChar } ++
                            namesGroup.leadingHybridChar.map { positionedHybridChar }
      positionedName(namesGroup.name) ++ namesPositions ++ hcharsPositions
    }

    def positionedName(nm: Name): Vector[Position] = {
      val typ = if (nm.genus) "genus" else "uninomial"
      Vector(positionedApproximation(nm.approximation),
             positionedSubGenus(nm.subgenus),
             positionedComparison(nm.comparison)).flatten ++
        positionedUninomial(typ, nm.uninomial) ++
        nm.species.map(positionedSpecies).orZero ++
        nm.infraspecies.map(positionedInfraspeciesGroup).orZero
    }

    def positionedHybridChar(hybridChar: HybridChar): Position =
      Position("hybrid_char", hybridChar.pos.start, hybridChar.pos.end)

    def positionedApproximation(approximation: Option[Approximation]): Option[Position] =
      approximation.map { app =>
        Position("annotation_identification", app.pos.start, app.pos.end)
      }

    def positionedComparison(comparison: Option[Comparison]): Option[Position] =
      comparison.map { c =>
        Position("annotation_identification", c.pos.start, c.pos.end)
      }

    def positionedRank(rank: Option[Rank]): Option[Position] =
      for (r <- rank; p <- r.pos.isDefined.option(r.pos))
        yield Position("rank", p.start, p.end)

    def positionedUninomial(typ: String, u: Uninomial): Vector[Position] =
      if (u.implied) Vector.empty
      else {
        Vector(Position(typ, u.pos.start, u.pos.end).some,
               positionedRank(u.rank)).flatten ++
          u.parent.map { positionedUninomial("uninomial", _) }.orZero ++
          u.authorship.map(positionedAuthorship).orZero
      }

    def positionedSubGenus(subGenus: Option[SubGenus]): Option[Position] =
      subGenus.map { sg =>
        Position("infrageneric_epithet", sg.pos.start, sg.pos.end)
      }

    def positionedSpecies(sp: Species): Vector[Position] =
      Position("specific_epithet", sp.pos.start, sp.pos.end) +:
        sp.authorship.map(positionedAuthorship).orZero

    def positionedInfraspecies(is: Infraspecies): Vector[Position] =
      Vector(Position("infraspecific_epithet", is.pos.start, is.pos.end).some,
             positionedRank(is.rank)).flatten ++
        is.authorship.map(positionedAuthorship).orZero

    def positionedInfraspeciesGroup(isg: InfraspeciesGroup): Vector[Position] =
      isg.group.flatMap(positionedInfraspecies).toVector

    def positionedYear(y: Year) = {
      val yearNodeName = if (y.approximate) "approximate_year" else "year"
      Position(yearNodeName, y.pos.start, y.alpha.getOrElse(y.pos).end)
    }

    def positionedAuthorship(as: Authorship): Vector[Position] = {
      def positionedAuthor(a: Author): Vector[Position] = {
        val authorWord = a.words.map { w =>
          Position("author_word", w.pos.start, w.pos.end) }.toVector
        val filius = a.filius.map { f =>
          Position("author_word_filius", f.pos.start, f.pos.end) }.toVector
        authorWord ++ filius
      }
      def positionedAuthorsTeam(at: AuthorsTeam): Vector[Position] =
        at.authors.flatMap(positionedAuthor).toVector ++
          at.year.map(positionedYear).toVector
      def positionedAuthorsGroup(ag: AuthorsGroup): Vector[Position] =
        positionedAuthorsTeam(ag.authors) ++
          ag.authorsEx.map(positionedAuthorsTeam).orZero

      as.basionym.map(positionedAuthorsGroup).orZero ++
        as.combination.map(positionedAuthorsGroup).orZero
    }

    result.scientificName.namesGroup.map { ng =>
      positionedNamesGroup(ng).sortBy { _.start }
    }.orZero
  }
}

object Positions {
  case class Position(nodeName: String, start: Int, end: Int)
}
