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
  isVirus: Boolean = false,
  quality: Int = 1,
  unparsedTail: Option[String] = None) extends AstNode {

  val isHybrid = namesGroup.map { ng =>
    ng.name.size > 1 || ng.hybrid.isDefined
  }
  val surrogate: Boolean = {
    val isBold = unparsedTail.map {
      g => Disj(g.contains("BOLD") || g.contains("Bold"))
    }
    val isAnnot = namesGroup.map { ng => Disj(ng.name.exists { n =>
        n.approximation.isDefined || n.comparison.isDefined
      })
    }
    Disj.unwrap(~(isBold |+| isAnnot))
  }
  val authorship: Option[Authorship] =
    namesGroup.flatMap { x => (x.name.size == 1).option {
      val name = namesGroup.get.name.head
      val infraspeciesAuthorship =
        name.infraspecies.map { _.group.last.authorship }
      val speciesAuthorship = name.species.map { _.authorship }
      val uninomialAuthorship = name.uninomial.authorship.map { _.some }
      val authorship =
        infraspeciesAuthorship <+> speciesAuthorship <+> uninomialAuthorship
      authorship.flatten
    }}.flatten
  val year: Option[Year] = namesGroup.flatMap { x => (x.name.size == 1).option {
    val name = namesGroup.get.name.head
    val infraspeciesYear =
      name.infraspecies.flatMap {
        _.group.last.authorship.flatMap { _.authors.year }
      }
    val speciesYear =
      name.species.flatMap { _.authorship.flatMap { _.authors.year } }
    val uninomialYear = name.uninomial.authorship.flatMap { _.authors.year }
    infraspeciesYear <+> speciesYear <+> uninomialYear
  }}.flatten
}

case class NamesGroup(
  name: Seq[Name],
  hybrid: Option[HybridChar] = None) extends AstNode {

  val pos: CapturePosition =
    CapturePosition(name.head.pos.start, name.last.pos.end)
}

case class Name(
  uninomial: Uninomial,
  subgenus: Option[SubGenus] = None,
  species: Option[Species] = None,
  infraspecies: Option[InfraspeciesGroup] = None,
  comparison: Option[Comparison] = None,
  approximation: Option[Approximation] = None,
  ignored: Option[String] = None,
  private val genusParsed: Boolean = false) extends AstNode {

  val genus: Boolean = genusParsed || species.isDefined ||
                       approximation.isDefined
  val pos: CapturePosition = {
    val nodes = Vector(uninomial.some, subgenus, species,
                       infraspecies, comparison, approximation).flatten
    CapturePosition(nodes.sortBy { _.pos.start }.head.pos.start,
               nodes.sortBy { -_.pos.end }.last.pos.end)
  }
}

case class HybridChar(pos: CapturePosition) extends AstNode

case class Comparison(pos: CapturePosition) extends AstNode

case class Approximation(pos: CapturePosition) extends AstNode

case class Rank(pos: CapturePosition, typ: Option[String] = None) extends AstNode

case class Uninomial(
  pos: CapturePosition,
  authorship: Option[Authorship] = None,
  rank: Option[Rank] = None,
  parent: Option[Uninomial] = None,
  implied: Boolean = false) extends AstNode

case class UninomialWord(pos: CapturePosition) extends AstNode

case class SpeciesWord(pos: CapturePosition) extends AstNode

case class SubGenus(word: UninomialWord) extends AstNode {
  val pos = word.pos
}

case class Species(word: SpeciesWord,
                   authorship: Option[Authorship] = None) extends AstNode {
  val pos = word.pos
}

case class Infraspecies(word: SpeciesWord,
                        rank: Option[Rank] = None,
                        authorship: Option[Authorship]) extends AstNode {
  val pos = word.pos
}

case class InfraspeciesGroup(group: Seq[Infraspecies]) extends AstNode {
  val pos = CapturePosition(group.head.pos.start, group.last.pos.end)
}

case class Year(pos: CapturePosition,
                alpha: Option[CapturePosition] = None,
                rangeEnd: Option[CapturePosition] = None,
                approximate: Boolean = false) extends AstNode {
  val isRange = rangeEnd.isDefined
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

  val pos = {
    val end = filius.getOrElse(words.last).pos.end
    CapturePosition(words.head.pos.start, end)
  }
}

case class AuthorsTeam(authors: Seq[Author]) extends AstNode {

  val pos: CapturePosition = {
    CapturePosition(authors.sortBy { _.pos.start }.head.pos.start,
                    authors.sortBy { _.pos.end }.head.pos.start)
  }
}

case class AuthorsGroup(authors: AuthorsTeam,
                        authorsEx: Option[AuthorsTeam] = None,
                        year: Option[Year] = None) extends AstNode {

  val pos: CapturePosition = {
    val nodes = Vector(authors.some, authorsEx, year).flatten
    CapturePosition(nodes.sortBy { _.pos.start }.head.pos.start,
                    nodes.sortBy { _.pos.end }.last.pos.end)
  }
}

case class Authorship(authors: AuthorsGroup,
                      combination: Option[AuthorsGroup] = None,
                      inparenthesis: Boolean = false,
                      private val basionymParsed: Boolean = false) extends AstNode {

  val pos: CapturePosition = {
    val nodes = Vector(authors.some, combination).flatten
    CapturePosition(nodes.sortBy { _.pos.start }.head.pos.start,
                    nodes.sortBy { _.pos.end }.last.pos.end)
  }
  val basionym: Option[AuthorsGroup] =
    (basionymParsed || combination.isEmpty).option(authors)
}
