package org.globalnames
package parser
package formatters

import scalaz.syntax.traverse._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.std.option._
import scalaz.std.vector._

class PositionsRenderer(result: Result) {

  import PositionsRenderer.Position

  def positioned: Seq[Position] = {
    def positionedNamesGroup(namesGroup: ast.NamesGroup): Vector[Position] = {
      val (hchars, names) = namesGroup.hybridParts.unzip
      val namesPositions = names.flatMap { _.map { positionedName } }.flatten
      val hcharsPositions = hchars.map { positionedHybridChar } ++
                            namesGroup.leadingHybridChar.map { positionedHybridChar }
      positionedName(namesGroup.name) ++ namesPositions ++ hcharsPositions
    }

    def positionedName(nm: ast.Name): Vector[Position] = {
      val typ = if (nm.genus) "genus" else "uninomial"
      Vector(positionedApproximation(nm.approximation),
             positionedSubGenus(nm.subgenus),
             positionedComparison(nm.comparison)).flatten ++
        positionedUninomial(typ, nm.uninomial) ++
        nm.species.map(positionedSpecies).orZero ++
        nm.infraspecies.map(positionedInfraspeciesGroup).orZero
    }

    def positionedHybridChar(hybridChar: ast.HybridChar): Position =
      Position("hybridChar", hybridChar.pos.start, hybridChar.pos.end)

    def positionedApproximation(approximation: Option[ast.Approximation]): Option[Position] =
      approximation.map { app =>
        Position("annotationIdentification", app.pos.start, app.pos.end)
      }

    def positionedComparison(comparison: Option[ast.Comparison]): Option[Position] =
      comparison.map { c =>
        Position("annotationIdentification", c.pos.start, c.pos.end)
      }

    def positionedRank(rank: Option[ast.Rank]): Option[Position] =
      for (r <- rank; p <- r.pos.isDefined.option(r.pos))
        yield Position("rank", p.start, p.end)

    def positionedUninomial(typ: String, u: ast.Uninomial): Vector[Position] =
      if (u.implied) Vector.empty
      else {
        Vector(Position(typ, u.pos.start, u.pos.end).some,
               positionedRank(u.rank)).flatten ++
          u.parent.map { positionedUninomial("uninomial", _) }.orZero ++
          u.authorship.map(positionedAuthorship).orZero
      }

    def positionedSubGenus(subGenus: Option[ast.SubGenus]): Option[Position] =
      subGenus.map { sg =>
        Position("infragenericEpithet", sg.pos.start, sg.pos.end)
      }

    def positionedSpecies(sp: ast.Species): Vector[Position] =
      Position("specificEpithet", sp.pos.start, sp.pos.end) +:
        sp.authorship.map(positionedAuthorship).orZero

    def positionedInfraspecies(is: ast.Infraspecies): Vector[Position] =
      Vector(Position("infraspecificEpithet", is.pos.start, is.pos.end).some,
             positionedRank(is.rank)).flatten ++
        is.authorship.map(positionedAuthorship).orZero

    def positionedInfraspeciesGroup(isg: ast.InfraspeciesGroup): Vector[Position] =
      isg.group.flatMap(positionedInfraspecies).toVector

    def positionedYear(y: ast.Year) = {
      val yearNodeName = if (y.approximate) "approximateYear" else "year"
      Position(yearNodeName, y.pos.start, y.alpha.getOrElse(y.pos).end)
    }

    def positionedAuthorship(as: ast.Authorship): Vector[Position] = {
      def positionedAuthor(a: ast.Author): Vector[Position] = {
        val authorWord = a.words.map { w =>
          Position("authorWord", w.pos.start, w.pos.end) }.toVector
        val filius = a.filius.map { f =>
          Position("authorWordFilius", f.pos.start, f.pos.end) }.toVector
        authorWord ++ filius
      }
      def positionedAuthorsTeam(at: ast.AuthorsTeam): Vector[Position] =
        at.authors.flatMap(positionedAuthor).toVector ++
          at.year.map(positionedYear).toVector
      def positionedAuthorsGroup(ag: ast.AuthorsGroup): Vector[Position] =
        positionedAuthorsTeam(ag.authors) ++
          ag.authorsEx.map(positionedAuthorsTeam).orZero

      as.basionym.map(positionedAuthorsGroup).orZero ++
        as.combination.map(positionedAuthorsGroup).orZero
    }

    result.scientificName.namesGroup.map { ng =>
      positionedNamesGroup(ng).sortWith { (p1, p2) =>
        if (p1.start == p2.start) {
          p1.end < p2.end
        } else {
          p1.start < p2.start
        }
      }
    }.orZero
  }
}

object PositionsRenderer {
  case class Position(nodeName: String, start: Int, end: Int)
}