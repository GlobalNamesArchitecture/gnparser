package org.globalnames
package parser
package formatters

import org.json4s.JsonAST.{JBool, JNothing}
import org.json4s.JsonDSL._
import org.json4s.{JObject, JString, JValue}

import scalaz.syntax.semigroup._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.std.string._
import scalaz.std.option._

class DetailsRenderer(result: parser.Result) {

  def detailed: JValue = {
    def detailedNamesGroup(namesGroup: ast.NamesGroup): JValue = {
      val hybs = namesGroup.hybridParts.flatMap { _._2.map { (_, namesGroup.name.some) } }
      ((namesGroup.name, None) +: hybs).map { case (n, fn) => detailedName(n, fn) }
    }

    def detailedName(nm: ast.Name, firstName: Option[ast.Name]): JValue = {
      val uninomialDetails = {
        val typ = if (nm.genus) "genus" else "uninomial"
        val typVal =
          if (nm.uninomial.implied) JNothing
          else {
            val firstNameUninomial =
              firstName.flatMap { fn => result.namesEqual(fn, nm).option { fn.uninomial } }
            detailedUninomial(nm.uninomial, firstNameUninomial)
          }
        typ -> typVal
      }

      val ignoredObj = nm.ignored.map {
        ign => JObject("ignored" -> JObject("value" -> JString(ign)))
      }.getOrElse(JObject())

      uninomialDetails ~
        ("specificEpithet" -> nm.species.map(detailedSpecies)) ~
        ("infragenericEpithet" -> nm.subgenus.map(detailedSubGenus)) ~
        ("infraspecificEpithets" ->
          nm.infraspecies.map(detailedInfraspeciesGroup)) ~
        ("annotationIdentification" ->
          (nm.approximation.map { result.stringOf } |+|
            nm.comparison.map { result.stringOf })) ~
        ignoredObj
    }

    def detailedUninomial(u: ast.Uninomial, firstName: Option[ast.Uninomial]): JValue = {
      val rankStr = u.rank.map { r => r.typ.getOrElse(result.stringOf(r)) }
      val fnVal = firstName.map { fn =>
        Util.normalize(result.stringOf(fn))
      }.getOrElse(Util.normalize(result.stringOf(u)))

      ("value" -> fnVal) ~
        ("rank" -> rankStr) ~
        ("parent" -> u.parent.map { p => Util.normalize(result.stringOf(p)) }) ~
        u.authorship.map(detailedAuthorship).getOrElse(JObject())
    }

    def detailedSubGenus(sg: ast.SubGenus): JValue =
      "value" -> Util.normalize(result.stringOf(sg.word))

    def detailedSpecies(sp: ast.Species): JValue =
      ("value" -> Util.normalize(result.stringOf(sp))) ~
        sp.authorship.map(detailedAuthorship).getOrElse(JObject())

    def detailedInfraspecies(is: ast.Infraspecies): JValue = {
      val rankStr = is.rank.map { r => r.typ.getOrElse(result.stringOf(r)) }
      ("value" -> Util.normalize(result.stringOf(is))) ~
        ("rank" -> rankStr) ~
        is.authorship.map(detailedAuthorship).getOrElse(JObject())
    }

    def detailedInfraspeciesGroup(isg: ast.InfraspeciesGroup): JValue =
      isg.group.map(detailedInfraspecies)

    def detailedYear(y: ast.Year): JValue = {
      val approximate: JObject =
        if (y.approximate) "approximate" -> JBool(true)
        else JObject()
      ("value" -> result.stringOf(y)) ~ approximate
    }

    def detailedAuthorship(as: ast.Authorship): JObject = {
      def detailedAuthor(a: ast.Author): String = result.normalizedAuthor(a)
      def detailedAuthorsTeam(at: ast.AuthorsTeam): JObject = {
        val authors = "authors" -> at.authors.map(detailedAuthor)
        val years = at.years.map { detailedYear }
        val yearsJson = "years" -> (years.isEmpty ? (JNothing: JValue) | years)
        authors ~ yearsJson
      }
      def detailedAuthorsGroup(ag: ast.AuthorsGroup): JObject =
        detailedAuthorsTeam(ag.authors) ~
          ("exAuthors" -> ag.authorsEx.map { at => detailedAuthorsTeam(at) }) ~
          ("emendAuthors" -> ag.authorsEmend.map { at => detailedAuthorsTeam(at) })

      "authorship" -> (
        ("value" -> result.normalizedAuthorship(as)) ~
          ("basionymAuthorship" -> as.basionym.map(detailedAuthorsGroup)) ~
          ("combinationAuthorship" -> as.combination.map(detailedAuthorsGroup))
      )
    }

    result.scientificName.namesGroup.map(detailedNamesGroup)
  }
}
