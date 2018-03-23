package org.globalnames.parser

import org.parboiled2.CapturePosition

import scalaz._
import Tags.{Disjunction => Disj}
import Scalaz._

trait AstNode {
  val pos: CapturePosition
}

case class ScientificName(
  pos: CapturePosition = CapturePosition.empty,
  namesGroup: Option[NamesGroup] = None,
  quality: Int = 1,
  unparsedTail: Option[String] = None,
  surrogatePreprocessed: Boolean = false) extends AstNode {

  val hybrid: Option[Boolean] = namesGroup.map { ng =>
    ng.namedHybrid || ng.hybridParts.nonEmpty
  }

  val bacteria: Boolean = namesGroup.exists { ng =>
    val nameBacteria = ng.name.bacteria
    val nameRestBacteria = ng.names.map { nOpt => nOpt.exists { _.bacteria } }.exists(identity)
    nameBacteria || nameRestBacteria
  }

  val surrogate: Boolean = {
    if (surrogatePreprocessed) true
    else {
      val isBold = unparsedTail.map {
        g => Disj(g.contains("BOLD") || g.contains("Bold"))
      }
      val isAnnot = namesGroup.map { ng =>
        Disj(ng.names.exists { n =>
          n.isDefined && (n.get.approximation.isDefined || n.get.comparison.isDefined)
        })
      }
      Disj.unwrap(~(isBold |+| isAnnot))
    }
  }

  val authorship: Option[Authorship] = namesGroup.flatMap { ng =>
    val infraspeciesAuthorship = ng.name.infraspecies.map { _.group.last.authorship }
    val speciesAuthorship = ng.name.species.map { _.authorship }
    val uninomialAuthorship = ng.name.uninomial.authorship.map { _.some }
    val authorship = infraspeciesAuthorship <+> speciesAuthorship <+> uninomialAuthorship
    authorship.flatten
  }

  val year: Option[Year] = namesGroup.flatMap { ng =>
    val infraspeciesYear = ng.name.infraspecies.flatMap {
      _.group.last.authorship.flatMap { _.authors.year }
    }
    val speciesYear = ng.name.species.flatMap { _.authorship.flatMap { _.authors.year } }
    val uninomialYear = ng.name.uninomial.authorship.flatMap { _.authors.year }
    infraspeciesYear <+> speciesYear <+> uninomialYear
  }
}

case class NamesGroup(name: Name,
                      hybridParts: Seq[(HybridChar, Option[Name])],
                      leadingHybridChar: Option[HybridChar]) extends AstNode {
  val namedHybrid: Boolean = leadingHybridChar.isDefined

  assert(!(hybridParts.nonEmpty && namedHybrid))

  val names: Seq[Option[Name]] = name.some +: hybridParts.map { _._2 }
  val pos: CapturePosition = {
    val end =
      if (hybridParts.isEmpty) name.pos.end
      else hybridParts.last match {
        case (_, Some(n)) => n.pos.end
        case (hc, None) => hc.pos.end
      }
    CapturePosition(name.pos.start, end)
  }
}

case class Name(
  uninomial: Uninomial,
  subgenus: Option[SubGenus] = None,
  species: Option[Species] = None,
  infraspecies: Option[InfraspeciesGroup] = None,
  comparison: Option[Comparison] = None,
  approximation: Option[Approximation] = None,
  ignored: Option[String] = None,
  bacteria: Boolean = false,
  private val genusParsed: Boolean = false) extends AstNode {

  val genus: Boolean = genusParsed || species.isDefined || approximation.isDefined
  val pos: CapturePosition = {
    val nodes = Vector(uninomial.some, subgenus, species,
                       infraspecies, comparison, approximation).flatten
    CapturePosition(nodes.minBy(_.pos.start).pos.start,
                    nodes.maxBy(-_.pos.end).pos.end)
  }
}

case class HybridChar(pos: CapturePosition) extends AstNode

case class Comparison(pos: CapturePosition) extends AstNode

case class Approximation(pos: CapturePosition) extends AstNode

case class Rank(pos: CapturePosition, typ: Option[String] = None) extends AstNode

case class Uninomial(
  word: UninomialWord,
  authorship: Option[Authorship] = None,
  rank: Option[Rank] = None,
  parent: Option[Uninomial] = None,
  implied: Boolean = false) extends AstNode {
  val pos: CapturePosition = word.pos
}

case class UninomialWord(pos: CapturePosition) extends AstNode

case class SpeciesWord(pos: CapturePosition) extends AstNode

case class SubGenus(word: UninomialWord) extends AstNode {
  val pos: CapturePosition = word.pos
}

case class Species(word: SpeciesWord,
                   authorship: Option[Authorship] = None) extends AstNode {
  val pos: CapturePosition = word.pos
}

case class Infraspecies(word: SpeciesWord,
                        rank: Option[Rank] = None,
                        authorship: Option[Authorship]) extends AstNode {
  val pos: CapturePosition = word.pos
}

case class InfraspeciesGroup(group: Seq[Infraspecies]) extends AstNode {
  val pos = CapturePosition(group.head.pos.start, group.last.pos.end)
}

case class Year(pos: CapturePosition,
                alpha: Option[CapturePosition] = None,
                rangeEnd: Option[CapturePosition] = None,
                approximate: Boolean = false) extends AstNode {
  val isRange: Boolean = rangeEnd.isDefined
}

sealed trait AuthorWordSeparator
object AuthorWordSeparator {
  case object Dash extends AuthorWordSeparator
  case object Space extends AuthorWordSeparator
  case object None extends AuthorWordSeparator
}

case class AuthorWord(pos: CapturePosition,
                      separator: AuthorWordSeparator = AuthorWordSeparator.None) extends AstNode

case class Author(words: Seq[AuthorWord],
                  anon: Boolean = false,
                  filius: Option[AuthorWord] = None) extends AstNode {

  val pos: CapturePosition = {
    val end = filius.getOrElse(words.last).pos.end
    CapturePosition(words.head.pos.start, end)
  }
}

case class AuthorsTeam(authors: Seq[Author]) extends AstNode {

  val pos: CapturePosition = {
    CapturePosition(authors.minBy { _.pos.start }.pos.start,
                    authors.minBy { _.pos.end }.pos.start)
  }
}

case class AuthorsGroup(authors: AuthorsTeam,
                        authorsEx: Option[AuthorsTeam] = None,
                        year: Option[Year] = None) extends AstNode {

  val pos: CapturePosition = {
    val nodes = Vector(authors.some, authorsEx, year).flatten
    CapturePosition(nodes.minBy { _.pos.start }.pos.start,
                    nodes.maxBy { _.pos.end }.pos.end)
  }
}

case class Authorship(authors: AuthorsGroup,
                      combination: Option[AuthorsGroup] = None,
                      inparenthesis: Boolean = false,
                      private val basionymParsed: Boolean = false) extends AstNode {

  val pos: CapturePosition = {
    val nodes = Vector(authors.some, combination).flatten
    CapturePosition(nodes.minBy { _.pos.start }.pos.start,
                    nodes.maxBy { _.pos.end }.pos.end)
  }
  val basionym: Option[AuthorsGroup] =
    (basionymParsed || combination.isEmpty).option(authors)
}
