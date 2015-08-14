package org.globalnames.formatters

import org.globalnames.parser._
import org.json4s.JsonDSL._
import org.json4s.{JArray, JNothing, JObject, JString, JValue}

import scalaz.Scalaz._

object Details {
  def format(scientificName: ScientificName): JValue = scientificName match {
    case ScientificName(_, None, _) => JNothing
    case ScientificName(_, Some(ng), _) => format(ng)
  }

  def format(namesGroup: NamesGroup): JValue =
    JArray(namesGroup.name.map(format).toList)

  def format(nm: Name): JValue = {
    val typ = if (nm.genus) "genus" else "uninomial"
    val ignoredObj = nm.ignored.map { ign => JObject("ignored" -> JObject("string" -> JString(ign))) }
                       .getOrElse(JObject())

    (typ -> format(nm.uninomial)) ~
      ("species" -> nm.species.map(format)) ~
      ("infragenus" -> nm.subgenus.map(format)) ~
      ("infraspecies" -> nm.infraspecies.map(format)) ~
      ("annotation_identification" -> (nm.approximation |+| nm.comparison)) ~
      ignoredObj
  }

  def format(u: Uninomial): JValue =
    ("string" -> Util.norm(u.str)) ~ u.authorship.map(format).getOrElse(JObject())

  def format(sg: SubGenus): JValue =
    "string" -> Util.norm(sg.subgenus.str)

  def format(sp: Species): JValue =
    ("string" -> Util.norm(sp.str)) ~ sp.authorship.map(format).getOrElse(JObject())

  def format(is: Infraspecies): JValue =
    ("string" -> Util.norm(is.str)) ~ ("rank" -> is.rank.getOrElse("n/a")) ~
      is.authorship.map(format).getOrElse(JObject())

  def format(isg: InfraspeciesGroup): JValue = isg.group.map(format).toList

  def format(y: Year): JValue = y.str

  def format(as: Authorship): JObject = {
    def formatAuthor(a: Author): String = if (a.filius) a.str + " f." else a.str
    def formatAuthorsTeam(at: AuthorsTeam): JObject =
      "author" -> at.authors.map(x => JString(formatAuthor(x))).toList
    def formatAuthorsGroup(ag: AuthorsGroup): JObject =
      formatAuthorsTeam(ag.authors) ~
        ("year" -> ag.year.map(format)) ~
        ("exAuthorTeam" -> ag.authorsEx.map(formatAuthorsTeam))
    
    ("authorship" -> Normalizer.format(as)) ~
      ("basionymAuthorTeam" -> as.basionym.map(formatAuthorsGroup)) ~
      ("combinationAuthorTeam" -> as.combination.map(formatAuthorsGroup))
  }
}
