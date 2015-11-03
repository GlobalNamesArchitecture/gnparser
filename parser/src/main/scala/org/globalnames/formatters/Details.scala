package org.globalnames.formatters

import org.globalnames.parser._
import org.json4s.JsonAST.JNothing
import org.json4s.JsonDSL._
import org.json4s.{JObject, JString, JValue}

import scalaz.Scalaz._

trait Details { parsedResult: ScientificNameParser.Result =>

  def detailed: JValue = {
    def detailedNamesGroup(namesGroup: NamesGroup): JValue =
      namesGroup.name.map { detailedName }

    def detailedName(nm: Name): JValue = {
      val uninomialDetails = {
        val typ = if (nm.genus) "genus" else "uninomial"
        typ -> (if (nm.uninomial.implied) JNothing
                else detailedUninomial(nm.uninomial))
      }

      val ignoredObj = nm.ignored.map {
          ign => JObject("ignored" -> JObject("string" -> JString(ign))) }
        .getOrElse(JObject())

      uninomialDetails ~
        ("species" -> nm.species.map(detailedSpecies)) ~
        ("infragenus" -> nm.subgenus.map(detailedSubGenus)) ~
        ("infraspecies" -> nm.infraspecies.map(detailedInfraspeciesGroup)) ~
        ("annotation_identification" ->
          (nm.approximation.map { stringOf } |+|
            nm.comparison.map { stringOf })) ~
        ignoredObj
    }

    def detailedUninomial(u: Uninomial): JValue = {
      val rankStr =
        u.rank
         .map { r => r.typ.getOrElse(stringOf(r)) }
      ("string" -> Util.norm(stringOf(u))) ~
        ("rank" -> rankStr) ~
        ("parent" -> u.parent.map { p => Util.norm(stringOf(p)) }) ~
        u.authorship.map(detailedAuthorship).getOrElse(JObject())
    }

    def detailedSubGenus(sg: SubGenus): JValue =
      "string" -> Util.norm(stringOf(sg.subgenus))

    def detailedSpecies(sp: Species): JValue =
      ("string" -> Util.norm(stringOf(sp))) ~
        sp.authorship.map(detailedAuthorship).getOrElse(JObject())

    def detailedInfraspecies(is: Infraspecies): JValue = {
      val rankStr =
        is.rank
          .map { r => r.typ.getOrElse(stringOf(r)) }
          .getOrElse("n/a")
      ("string" -> Util.norm(stringOf(is))) ~
        ("rank" -> rankStr) ~
        is.authorship.map(detailedAuthorship).getOrElse(JObject())
    }

    def detailedInfraspeciesGroup(isg: InfraspeciesGroup): JValue =
      isg.group.map(detailedInfraspecies)

    def detailedYear(y: Year): JValue =
      ("str" -> stringOf(y)) ~
        ("approximate" -> y.approximate)

    def detailedAuthorship(as: Authorship): JObject = {
      def detailedAuthor(a: Author): String = normalizedAuthor(a)
      def detailedAuthorsTeam(at: AuthorsTeam): JObject =
        "author" -> at.authors.map(detailedAuthor)
      def detailedAuthorsGroup(ag: AuthorsGroup): JObject =
        detailedAuthorsTeam(ag.authors) ~
          ("year" -> ag.year.map(detailedYear)) ~
          ("ex_author_team" -> ag.authorsEx.map(detailedAuthorsTeam))

      ("authorship" -> parsedResult.normalizedAuthorship(as)) ~
        ("basionym_author_team" -> as.basionym.map(detailedAuthorsGroup)) ~
        ("combination_author_team" -> as.combination.map(detailedAuthorsGroup))
    }

    parsedResult.scientificName.namesGroup.map(detailedNamesGroup)
  }
}
