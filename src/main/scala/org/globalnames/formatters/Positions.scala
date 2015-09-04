package org.globalnames.formatters

import org.globalnames.parser._

import scalaz.Scalaz._

trait Positions { parsedResult: ScientificNameParser.Result =>

  import Positions.Position

  def positioned: Seq[Position] = {
    def positionedNamesGroup(namesGroup: NamesGroup): Vector[Position] =
      namesGroup.name.flatMap(positionedName).toVector

    def positionedName(nm: Name): Vector[Position] = {
      val typ = if (nm.genus) "genus" else "uninomial"
      val positions =
        positionedUninomial(typ, nm.uninomial) ++
          nm.subgenus.map(positionedSubGenus).toVector ++
          ~nm.species.map(positionedSpecies) ++
          ~nm.infraspecies.map(positionedInfraspeciesGroup)
      positions.sortBy(_.start)
    }

    def positionedUninomial(typ: String, u: Uninomial): Vector[Position] =
      Position(typ, u.pos.start, u.pos.end) +:
        ~u.authorship.map(positionedAuthorship)

    def positionedSubGenus(sg: SubGenus): Position =
      Position("infragenus", sg.pos.start, sg.pos.end)

    def positionedSpecies(sp: Species): Vector[Position] =
      Position("species", sp.pos.start, sp.pos.end) +:
        ~sp.authorship.map(positionedAuthorship)

    def positionedInfraspecies(is: Infraspecies): Vector[Position] =
      Position("infraspecies", is.pos.start, is.pos.end) +:
        ~is.authorship.map(positionedAuthorship)

    def positionedInfraspeciesGroup(isg: InfraspeciesGroup): Vector[Position] =
      isg.group.flatMap(positionedInfraspecies).toVector

    def positionedYear(y: Year) =
      Position("year", y.pos.start, y.alpha.getOrElse(y.pos).end)

    def positionedAuthorship(as: Authorship): Vector[Position] = {
      def positionedAuthor(a: Author): Vector[Position] =
        a.words.map(p => Position("author_word", p.start, p.end)).toVector
      def positionedAuthorsTeam(at: AuthorsTeam): Vector[Position] =
        at.authors.flatMap(positionedAuthor).toVector
      def positionedAuthorsGroup(ag: AuthorsGroup): Vector[Position] =
        positionedAuthorsTeam(ag.authors) ++
          ag.year.map(positionedYear) ++
          ~ag.authorsEx.map(positionedAuthorsTeam)

      ~as.basionym.map(positionedAuthorsGroup) ++
        ~as.combination.map(positionedAuthorsGroup)
    }

    ~parsedResult.scientificName.namesGroup.map(positionedNamesGroup)
  }
}

object Positions {
  case class Position(nodeName: String, start: Int, end: Int)
}
