package org.globalnames.formatters

import org.globalnames.parser._
import org.json4s.JsonAST.{JBool, JNothing}
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
          ign => JObject("ignored" -> JObject("value" -> JString(ign))) }
        .getOrElse(JObject())

      uninomialDetails ~
        ("specific_epithet" -> nm.species.map(detailedSpecies)) ~
        ("infrageneric_epithet" -> nm.subgenus.map(detailedSubGenus)) ~
        ("infraspecific_epithets" ->
          nm.infraspecies.map(detailedInfraspeciesGroup)) ~
        ("annotation_identification" ->
          (nm.approximation.map { stringOf } |+|
            nm.comparison.map { stringOf })) ~
        ignoredObj
    }

    def detailedUninomial(u: Uninomial): JValue = {
      val rankStr =
        u.rank
         .map { r => r.typ.getOrElse(stringOf(r)) }
      ("value" -> Util.norm(stringOf(u))) ~
        ("rank" -> rankStr) ~
        ("parent" -> u.parent.map { p => Util.norm(stringOf(p)) }) ~
        u.authorship.map(detailedAuthorship).getOrElse(JObject())
    }

    def detailedSubGenus(sg: SubGenus): JValue =
      "value" -> Util.norm(stringOf(sg.word))

    def detailedSpecies(sp: Species): JValue =
      ("value" -> Util.norm(stringOf(sp))) ~
        sp.authorship.map(detailedAuthorship).getOrElse(JObject())

    def detailedInfraspecies(is: Infraspecies): JValue = {
      val rankStr = is.rank.map { r => r.typ.getOrElse(stringOf(r)) }
      ("value" -> Util.norm(stringOf(is))) ~
        ("rank" -> rankStr) ~
        is.authorship.map(detailedAuthorship).getOrElse(JObject())
    }

    def detailedInfraspeciesGroup(isg: InfraspeciesGroup): JValue =
      isg.group.map(detailedInfraspecies)

    def detailedYear(y: Year): JValue = {
      val approximate: JObject =
        if (y.approximate) "approximate" -> JBool(true)
        else JObject()
      ("value" -> stringOf(y)) ~ approximate
    }

    def detailedAuthorship(as: Authorship): JObject = {
      def detailedAuthor(a: Author): String = normalizedAuthor(a)
      def detailedAuthorsTeam(at: AuthorsTeam): JObject =
        "authors" -> at.authors.map(detailedAuthor)
      def detailedExAuthorsTeam(at: AuthorsTeam): Seq[String]  =
        at.authors.map(detailedAuthor)
      def detailedAuthorsGroup(ag: AuthorsGroup): JObject =
        detailedAuthorsTeam(ag.authors) ~
          ("year" -> ag.year.map(detailedYear)) ~
          ("ex_authors" -> ag.authorsEx.map(detailedExAuthorsTeam))

      "authorship" -> (
        ("value" -> parsedResult.normalizedAuthorship(as)) ~
          ("basionym_authorship" -> as.basionym.map(detailedAuthorsGroup)) ~
          ("combination_authorship" -> as.combination.map(detailedAuthorsGroup))
        )
    }

    parsedResult.scientificName.namesGroup.map(detailedNamesGroup)
  }
}
