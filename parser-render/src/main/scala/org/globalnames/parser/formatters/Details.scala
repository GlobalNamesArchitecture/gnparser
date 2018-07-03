package org.globalnames.parser
package formatters

import org.json4s.JsonAST.{JBool, JNothing}
import org.json4s.JsonDSL._
import org.json4s.{JObject, JString, JValue}
import scalaz.Scalaz._

class Details(parsedResult: Result) extends CommonOps {
  protected val unescapedInput: String = parsedResult.preprocessorResult.unescaped

  def detailed: JValue = {
    def detailedNamesGroup(namesGroup: NamesGroup): JValue = {
      val hybs = namesGroup.hybridParts.flatMap { _._2.map { (_, namesGroup.name.some) } }
      ((namesGroup.name, None) +: hybs).map { case (n, fn) => detailedName(n, fn) }
    }

    def detailedName(nm: Name, firstName: Option[Name]): JValue = {
      val uninomialDetails = {
        val typ = if (nm.genus) "genus" else "uninomial"
        val typVal =
          if (nm.uninomial.implied) JNothing
          else {
            val firstNameUninomial =
              firstName.flatMap { fn => namesEqual(fn, nm).option { fn.uninomial } }
            detailedUninomial(nm.uninomial, firstNameUninomial)
          }
        typ -> typVal
      }

      val ignoredObj = nm.ignored.map {
        ign => JObject("ignored" -> JObject("value" -> JString(ign)))
      }.getOrElse(JObject())

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

    def detailedUninomial(u: Uninomial, firstName: Option[Uninomial]): JValue = {
      val rankStr = u.rank.map { r => r.typ.getOrElse(stringOf(r)) }
      val fnVal = firstName.map { fn => Util.normalize(stringOf(fn)) }.getOrElse(Util.normalize(stringOf(u)))
      ("value" -> fnVal) ~
        ("rank" -> rankStr) ~
        ("parent" -> u.parent.map { p => Util.normalize(stringOf(p)) }) ~
        u.authorship.map(detailedAuthorship).getOrElse(JObject())
    }

    def detailedSubGenus(sg: SubGenus): JValue =
      "value" -> Util.normalize(stringOf(sg.word))

    def detailedSpecies(sp: Species): JValue =
      ("value" -> Util.normalize(stringOf(sp))) ~
        sp.authorship.map(detailedAuthorship).getOrElse(JObject())

    def detailedInfraspecies(is: Infraspecies): JValue = {
      val rankStr = is.rank.map { r => r.typ.getOrElse(stringOf(r)) }
      ("value" -> Util.normalize(stringOf(is))) ~
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
      def detailedAuthor(a: Author): String = parsedResult.normalizer.normalizedAuthor(a)
      def detailedAuthorsTeam(at: AuthorsTeam): JObject = {
        val res: JObject = "authors" -> at.authors.map(detailedAuthor)
        at.years.foldLeft(res) { (r, y) => r ~ ("year" -> detailedYear(y)) }
      }
      def detailedAuthorsGroup(ag: AuthorsGroup): JObject =
        detailedAuthorsTeam(ag.authors) ~
          ("ex_authors" -> ag.authorsEx.map { at => detailedAuthorsTeam(at) }) ~
          ("emend_authors" -> ag.authorsEmend.map { at => detailedAuthorsTeam(at) })

      "authorship" -> (
        ("value" -> parsedResult.normalizer.normalizedAuthorship(as)) ~
          ("basionym_authorship" -> as.basionym.map(detailedAuthorsGroup)) ~
          ("combination_authorship" -> as.combination.map(detailedAuthorsGroup))
      )
    }

    parsedResult.scientificName.namesGroup.map(detailedNamesGroup)
  }
}
