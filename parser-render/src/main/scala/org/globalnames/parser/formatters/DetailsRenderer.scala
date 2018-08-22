package org.globalnames
package parser
package formatters

import scalaz.syntax.semigroup._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.std.string._
import scalaz.std.option._
import formatters.{DetailsRenderer => dr}
import spray.json._

object DetailsRendererJsonProtocol extends DefaultJsonProtocol {

  implicit val ignoredFormat = jsonFormat1(dr.Ignored)

  implicit val yearFormat = jsonFormat2(dr.Year)

  implicit val subGenusFormat = jsonFormat1(dr.SubGenus)

  implicit val authorsTeamFormat = jsonFormat2(dr.AuthorsTeam)

  implicit val authorsGroupFormat = jsonFormat3(dr.AuthorsGroup)

  implicit val basionymAuthorsGroupFormat = jsonFormat4(dr.BasionymAuthorsGroup)

  implicit val combinationAuthorsGroupFormat = jsonFormat4(dr.CombinationAuthorsGroup)

  implicit val authorshipFormat = jsonFormat3(dr.Authorship)

  implicit val speciesFormat = jsonFormat2(dr.Species)

  implicit val infraSpeciesFormat = jsonFormat3(dr.Infraspecies)

  implicit val uninomialFormat = jsonFormat6(dr.Uninomial)

  implicit val nameFormat = jsonFormat7(dr.Name)

}

class DetailsRenderer(result: parser.Result) {

  private def year(y: ast.Year): dr.Year = {
    val approximate = y.approximate.option { true }
    dr.Year(result.stringOf(y), approximate)
  }

  private def infraspeciesGroup(isg: ast.InfraspeciesGroup): Seq[dr.Infraspecies] = {
    isg.group.map { infraspecies }
  }

  private def infraspecies(is: ast.Infraspecies): dr.Infraspecies = {
    val rankStr = is.rank.map { r => r.typ.getOrElse(result.stringOf(r)) }
    val isDR = dr.Infraspecies(
      value = Util.normalize(result.stringOf(is)),
      rank = rankStr,
      authorship = is.authorship.map { authorship }
    )
    isDR
  }

  private def author(a: ast.Author): String = {
    result.normalizedAuthor(a)
  }

  private def authorsTeam(at: ast.AuthorsTeam): dr.AuthorsTeam = {
    val yearsArr = at.years.map { y => year(y) }
    val atDR = dr.AuthorsTeam(
      authors = at.authors.map { author },
      years = yearsArr.nonEmpty ? yearsArr.some | None
    )
    atDR
  }

  private def authorsGroup(ag: ast.AuthorsGroup): dr.AuthorsGroup = {
    val authorsDR = authorsTeam(ag.authors)
    val exAuthorsDR = ag.authorsEx.map { at => authorsTeam(at) }
    val emendAuthorsDR = ag.authorsEmend.map { at => authorsTeam(at) }
    val agDR = dr.AuthorsGroup(
      authors = authorsDR,
      exAuthors = exAuthorsDR,
      emendAuthors = emendAuthorsDR
    )
    agDR
  }

  private def namesGroup(namesGroup: ast.NamesGroup): Seq[dr.Name] = {
    val hybs = for {
      (_, nmOpt) <- namesGroup.hybridParts
      nm <- nmOpt
    } yield (nm, namesGroup.name.some)
    val nms = for ((n, fn) <- (namesGroup.name, None) +: hybs) yield name(n, fn)
    nms
  }

  private def name(nm: ast.Name, firstName: Option[ast.Name]): dr.Name = {
    val implied = nm.uninomial.implied
    val isGenus = !implied && nm.genus
    val isUninomial = !implied && !nm.genus
    val uninomialDR = {
      val fnuniOpt = for {
        fn <- firstName
        firstNameUninomial <- result.namesEqual(fn, nm).option { fn.uninomial }
      } yield firstNameUninomial
      uninomial(nm.uninomial, fnuniOpt)
    }

    val ignoredDR = nm.ignored.map { ign => dr.Ignored(ign) }

    val annotIdent =
      nm.approximation.map { result.stringOf } |+| nm.comparison.map { result.stringOf }

    val nameDR = dr.Name(
      uninomial = isUninomial.option { uninomialDR },
      genus = isGenus.option { uninomialDR },
      specificEpithet = nm.species.map { species },
      infragenericEpithet = nm.subgenus.map { subGenus },
      infraspecificEpithets = nm.infraspecies.map { infraspeciesGroup },
      annotationIdentification = annotIdent,
      ignored = ignoredDR
    )
    nameDR
  }

  private def uninomial(uni: ast.Uninomial,
                        firstName: Option[ast.Uninomial]): dr.Uninomial = {
    val rankStr = uni.rank.map { r => r.typ.getOrElse(result.stringOf(r)) }
    val firstNameStr = firstName.map { fn =>
      Util.normalize(result.stringOf(fn))
    }.getOrElse(Util.normalize(result.stringOf(uni)))

    val uniDR = dr.Uninomial(
      value = firstNameStr,
      rank = rankStr,
      parent = uni.parent.map { p => Util.normalize(result.stringOf(p)) },
      authorship = uni.authorship.map { authorship }
    )
    uniDR
  }

  private def subGenus(sg: ast.SubGenus): dr.SubGenus = {
    dr.SubGenus(value = Util.normalize(result.stringOf(sg.word)))
  }

  private def species(sp: ast.Species): dr.Species = {
    val spDR = dr.Species(
      value = Util.normalize(result.stringOf(sp)),
      authorship = sp.authorship.map { authorship }
    )
    spDR
  }

  private def authorship(as: ast.Authorship): dr.Authorship = {
    val auDR = dr.Authorship(
      value = result.normalizedAuthorship(as),
      basionymAuthorship = as.basionym.map { ag => {
        val agDR = authorsGroup(ag)
        dr.BasionymAuthorsGroup(
          agDR.authors.authors, agDR.authors.years, agDR.exAuthors, agDR.emendAuthors
        )
      }},
      combinationAuthorship = as.combination.map { ag => {
        val agDR = authorsGroup(ag)
        dr.CombinationAuthorsGroup(
          agDR.authors.authors, agDR.authors.years, agDR.exAuthors, agDR.emendAuthors
        )
      }}
    )
    auDR
  }

  def details: Seq[DetailsRenderer.Name] = {
    val nameDRs = for {
      ngAST <- result.scientificName.namesGroup.toSeq
      nmDR <- namesGroup(ngAST)
    } yield nmDR
    nameDRs
  }
}

object DetailsRenderer {

  case class Name(uninomial: Option[Uninomial],
                  genus: Option[Uninomial],
                  specificEpithet: Option[Species],
                  infragenericEpithet: Option[SubGenus],
                  infraspecificEpithets: Option[Seq[Infraspecies]],
                  annotationIdentification: Option[String],
                  ignored: Option[Ignored]) {
    assert(!(uninomial.isDefined && genus.isDefined))
  }

  case class Authorship(value: Option[String],
                        basionymAuthorship: Option[BasionymAuthorsGroup],
                        combinationAuthorship: Option[CombinationAuthorsGroup])

  case class AuthorsTeam(authors: Seq[String],
                         years: Option[Seq[Year]])

  case class AuthorsGroup(authors: AuthorsTeam,
                          exAuthors: Option[AuthorsTeam],
                          emendAuthors: Option[AuthorsTeam])

  case class BasionymAuthorsGroup(authors: Seq[String],
                                  years: Option[Seq[Year]],
                                  exAuthors: Option[AuthorsTeam],
                                  emendAuthors: Option[AuthorsTeam])

  case class CombinationAuthorsGroup(authors: Seq[String],
                                     years: Option[Seq[Year]],
                                     exAuthors: Option[AuthorsTeam],
                                     emendAuthors: Option[AuthorsTeam])

  case class Uninomial(value: String,
                       rank: Option[String] = None,
                       typ: Option[String] = None,
                       specificEpithet: Option[Species] = None,
                       parent: Option[String] = None,
                       authorship: Option[Authorship] = None)

  case class SubGenus(value: String)

  case class Species(value: String,
                     authorship: Option[Authorship])

  case class Infraspecies(value: String,
                          rank: Option[String],
                          authorship: Option[Authorship])

  case class Year(value: String,
                  approximate: Option[Boolean])

  case class Ignored(value: String)
}
