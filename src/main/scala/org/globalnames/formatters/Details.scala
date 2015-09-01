package org.globalnames.formatters

import org.globalnames.parser._
import org.json4s.JsonDSL._
import org.json4s.{JObject, JString, JValue}

import scalaz.Scalaz._

trait Details { parsedResult: ScientificNameParser.Result
                  with Normalizer with Canonizer =>

  def detailed: JValue = {
    def detailedNamesGroup(namesGroup: NamesGroup): JValue = namesGroup.name.map(detailedName)

    def detailedName(nm: Name): JValue = {
      val typ = if (nm.genus) "genus" else "uninomial"
      val ignoredObj = nm.ignored.map {
          ign => JObject("ignored" -> JObject("string" -> JString(ign))) }
        .getOrElse(JObject())

      (typ -> detailedUninomial(nm.uninomial)) ~
        ("species" -> nm.species.map(detailedSpecies)) ~
        ("infragenus" -> nm.subgenus.map(detailedSubGenus)) ~
        ("infraspecies" -> nm.infraspecies.map(detailedInfraspeciesGroup)) ~
        ("annotation_identification" -> (nm.approximation |+| nm.comparison)) ~
        ignoredObj
    }

    def detailedUninomial(u: Uninomial): JValue =
      ("string" -> canonizedUninomial(u)) ~
        u.authorship.map(detailedAuthorship).getOrElse(JObject())

    def detailedSubGenus(sg: SubGenus): JValue =
      "string" -> Util.norm(input.substring(sg.subgenus.pos))

    def detailedSpecies(sp: Species): JValue =
      ("string" -> Util.norm(input.substring(sp.pos))) ~
        sp.authorship.map(detailedAuthorship).getOrElse(JObject())

    def detailedInfraspecies(is: Infraspecies): JValue = {
      ("string" -> Util.norm(input.substring(is.pos))) ~
        ("rank" -> is.rank.getOrElse("n/a")) ~
        is.authorship.map(detailedAuthorship).getOrElse(JObject())
    }

    def detailedInfraspeciesGroup(isg: InfraspeciesGroup): JValue =
      isg.group.map(detailedInfraspecies)

    def detailedYear(y: Year): JValue =
      ("str" -> parsedResult.normalizedYear(y)) ~
        detailedPos(y.pos.start, y.alpha.getOrElse(y.pos).end)

    def detailedPos(start: Int, end: Int): JObject =
      "pos" -> (("start" -> start) ~ ("end" -> end))

    def detailedAuthorship(as: Authorship): JObject = {
      def detailedAuthor(a: Author): String = {
        if (a.filius) a.str + " f."
        else if (a.anon) "unknown"
        else a.str
      }
      def detailedAuthorsTeam(at: AuthorsTeam): JObject =
        "author" -> at.authors.map(detailedAuthor)
      def detailedAuthorsGroup(ag: AuthorsGroup): JObject =
        detailedAuthorsTeam(ag.authors) ~
          ("year" -> ag.year.map(detailedYear)) ~
          ("exAuthorTeam" -> ag.authorsEx.map(detailedAuthorsTeam))

      ("authorship" -> parsedResult.normalizedAuthorship(as)) ~
        ("basionymAuthorTeam" -> as.basionym.map(detailedAuthorsGroup)) ~
        ("combinationAuthorTeam" -> as.combination.map(detailedAuthorsGroup))
    }

    parsedResult.scientificName.namesGroup.map(detailedNamesGroup)
  }
}
