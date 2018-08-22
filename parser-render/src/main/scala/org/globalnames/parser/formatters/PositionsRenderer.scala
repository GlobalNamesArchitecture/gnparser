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

  private def namesGroup(namesGroup: ast.NamesGroup): Vector[Position] = {
    val (hchars, names) = namesGroup.hybridParts.unzip
    val namesPositions = names.flatMap { _.map { name } }.flatten
    val hcharsPositions = hchars.map { hybridChar } ++
                          namesGroup.leadingHybridChar.map { hybridChar }
    name(namesGroup.name) ++ namesPositions ++ hcharsPositions
  }

  private def name(nm: ast.Name): Vector[Position] = {
    val typ = if (nm.genus) "genus" else "uninomial"
    Vector(approximation(nm.approximation),
           subGenus(nm.subgenus),
           comparison(nm.comparison)).flatten ++
      uninomial(typ, nm.uninomial) ++
      nm.species.map(species).orZero ++
      nm.infraspecies.map(infraspeciesGroup).orZero
  }

  private def hybridChar(hybridChar: ast.HybridChar): Position = {
    Position("hybridChar", hybridChar.pos.start, hybridChar.pos.end)
  }

  private def approximation(approximation: Option[ast.Approximation]): Option[Position] = {
    approximation.map { app =>
      Position("annotationIdentification", app.pos.start, app.pos.end)
    }
  }

  private def comparison(comparison: Option[ast.Comparison]): Option[Position] = {
    comparison.map { c =>
      Position("annotationIdentification", c.pos.start, c.pos.end)
    }
  }

  private def rank(rank: Option[ast.Rank]): Option[Position] = {
    for {
      r <- rank
      p <- r.pos.isDefined.option(r.pos)
    } yield Position("rank", p.start, p.end)
  }

  private def uninomial(typ: String, u: ast.Uninomial): Vector[Position] = {
    if (u.implied) Vector.empty
    else {
      Vector(Position(typ, u.pos.start, u.pos.end).some,
             rank(u.rank)).flatten ++
        u.parent.map { uninomial("uninomial", _) }.orZero ++
        u.authorship.map(authorship).orZero
    }
  }

  private def subGenus(subGenus: Option[ast.SubGenus]): Option[Position] = {
    subGenus.map { sg =>
      Position("infragenericEpithet", sg.pos.start, sg.pos.end)
    }
  }

  private def species(sp: ast.Species): Vector[Position] = {
    Position("specificEpithet", sp.pos.start, sp.pos.end) +:
      sp.authorship.map(authorship).orZero
  }

  private def infraspecies(is: ast.Infraspecies): Vector[Position] = {
    Vector(Position("infraspecificEpithet", is.pos.start, is.pos.end).some,
           rank(is.rank)).flatten ++
      is.authorship.map(authorship).orZero
  }

  private def infraspeciesGroup(isg: ast.InfraspeciesGroup): Vector[Position] = {
    isg.group.flatMap(infraspecies).toVector
  }

  private def year(y: ast.Year): Position = {
    val yearNodeName = if (y.approximate) "approximateYear" else "year"
    Position(yearNodeName, y.pos.start, y.alpha.getOrElse(y.pos).end)
  }

  private def author(a: ast.Author): Vector[Position] = {
    val authorWord = a.words.map { w =>
      Position("authorWord", w.pos.start, w.pos.end) }.toVector
    val filius = a.filius.map { f =>
      Position("authorWordFilius", f.pos.start, f.pos.end) }.toVector
    authorWord ++ filius
  }

  private def authorsTeam(at: ast.AuthorsTeam): Vector[Position] = {
    at.authors.flatMap(author).toVector ++ at.year.map(year).toVector
  }

  private def authorsGroup(ag: ast.AuthorsGroup): Vector[Position] = {
    authorsTeam(ag.authors) ++ ag.authorsEx.map(authorsTeam).orZero
  }

  private def authorship(as: ast.Authorship): Vector[Position] = {
    as.basionym.map(authorsGroup).orZero ++
      as.combination.map(authorsGroup).orZero
  }

  def positioned: Seq[Position] = {
    result.scientificName.namesGroup.map { ng =>
      namesGroup(ng).sortWith { (p1, p2) =>
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
