package org.globalnames.formatters

import org.globalnames.parser._
import org.json4s.JsonDSL._
import org.json4s.{JObject, JString, JValue}

import scalaz.Scalaz._

object Details {
  def format(scientificName: ScientificName): JValue = {
    def formatNamesGroup(namesGroup: NamesGroup): JValue = namesGroup.name.map(formatName)

    def formatName(nm: Name): JValue = {
      val typ = if (nm.genus) "genus" else "uninomial"
      val ignoredObj = nm.ignored.map {
          ign => JObject("ignored" -> JObject("string" -> JString(ign))) }
        .getOrElse(JObject())

      (typ -> formatUninomial(nm.uninomial)) ~
        ("species" -> nm.species.map(formatSpecies)) ~
        ("infragenus" -> nm.subgenus.map(formatSubGenus)) ~
        ("infraspecies" -> nm.infraspecies.map(formatInfraspeciesGroup)) ~
        ("annotation_identification" -> (nm.approximation |+| nm.comparison)) ~
        ignoredObj
    }

    def formatUninomial(u: Uninomial): JValue =
      ("string" -> Util.norm(u.str)) ~
        u.authorship.map(formatAuthorship).getOrElse(JObject())

    def formatSubGenus(sg: SubGenus): JValue =
      "string" -> Util.norm(sg.subgenus.str)

    def formatSpecies(sp: Species): JValue =
      ("string" -> Util.norm(sp.str)) ~
        sp.authorship.map(formatAuthorship).getOrElse(JObject())

    def formatInfraspecies(is: Infraspecies): JValue =
      ("string" -> Util.norm(is.str)) ~ ("rank" -> is.rank.getOrElse("n/a")) ~
        is.authorship.map(formatAuthorship).getOrElse(JObject())

    def formatInfraspeciesGroup(isg: InfraspeciesGroup): JValue =
      isg.group.map(formatInfraspecies)

    def formatYear(y: Year): JValue =
      ("str" -> Normalizer.formatYear(scientificName)(y)) ~
        formatPos(y.pos.start, y.alpha.getOrElse(y.pos).end)

    def formatPos(start: Int, end: Int): JObject =
      "pos" -> (("start" -> start) ~ ("end" -> end))

    def formatAuthorship(as: Authorship): JObject = {
      def formatAuthor(a: Author): String = {
        if (a.filius) a.str + " f."
        else if (a.anon) "unknown"
        else a.str
      }
      def formatAuthorsTeam(at: AuthorsTeam): JObject =
        "author" -> at.authors.map(formatAuthor)
      def formatAuthorsGroup(ag: AuthorsGroup): JObject =
        formatAuthorsTeam(ag.authors) ~
          ("year" -> ag.year.map(formatYear)) ~
          ("exAuthorTeam" -> ag.authorsEx.map(formatAuthorsTeam))

      ("authorship" -> Normalizer.formatAuthorship(scientificName)(as)) ~
        ("basionymAuthorTeam" -> as.basionym.map(formatAuthorsGroup)) ~
        ("combinationAuthorTeam" -> as.combination.map(formatAuthorsGroup))
    }

    scientificName.namesGroup.map(formatNamesGroup)
  }
}
