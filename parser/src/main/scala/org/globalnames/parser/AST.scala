package org.globalnames.parser

import org.parboiled2.CapturePos

import scalaz._
import Tags.{Disjunction => Disj}
import Scalaz._

trait AstNode {
  val id: Int
  val pos: CapturePos
}

object AstNode {
  private var currentId = 0
  def id: Int = { currentId += 1; currentId }
}

case class ScientificName(
  namesGroup: Option[NamesGroup] = None,
  isVirus: Boolean = false,
  quality: Int = 1,
  garbage: Option[String] = None) {

  val isHybrid = namesGroup.map { ng => ng.name.size > 1 || ng.hybrid.isDefined }
  val surrogate: Boolean = {
    val isBold = garbage.map { g => Disj(g.contains("BOLD") || g.contains("Bold")) }
    val isAnnot = namesGroup.map { ng => Disj(ng.name.exists { n =>
        n.approximation.isDefined || n.comparison.isDefined
      })
    }
    Disj.unwrap(~(isBold |+| isAnnot))
  }
}

case class NamesGroup(
  id: Int,
  name: Seq[Name],
  hybrid: Option[HybridChar] = None) extends AstNode {

  val pos: CapturePos = CapturePos(name.head.pos.start, name.last.pos.end)
}

case class Name(
  id: Int,
  uninomial: Uninomial,
  subgenus: Option[SubGenus] = None,
  species: Option[Species] = None,
  infraspecies: Option[InfraspeciesGroup] = None,
  comparison: Option[Comparison] = None,
  approximation: Option[Approximation] = None,
  ignored: Option[String] = None,
  private val genusParsed: Boolean = false) extends AstNode {

  val genus: Boolean = genusParsed || species.isDefined || approximation.isDefined
  val pos: CapturePos = {
    val nodes = Vector(uninomial.some, subgenus, species,
                       infraspecies, comparison, approximation).flatten
    CapturePos(nodes.sortBy { _.pos.start }.head.pos.start,
               nodes.sortBy { -_.pos.end }.last.pos.end)
  }
}

case class HybridChar(
  id: Int,
  pos: CapturePos) extends AstNode

case class Comparison(
  id: Int,
  pos: CapturePos) extends AstNode

case class Approximation(
  id: Int,
  pos: CapturePos) extends AstNode

case class Rank(
  id: Int,
  pos: CapturePos,
  typ: Option[String] = None) extends AstNode

case class Uninomial(
  id: Int,
  pos: CapturePos,
  authorship: Option[Authorship] = None,
  rank: Option[Rank] = None,
  parent: Option[Uninomial] = None,
  implied: Boolean = false) extends AstNode

case class UninomialWord(
  id: Int,
  pos: CapturePos) extends AstNode

case class SpeciesWord(
  id: Int,
  pos: CapturePos) extends AstNode

case class SubGenus(
  id: Int,
  subgenus: UninomialWord) extends AstNode {

  val pos = subgenus.pos
}

case class Species(
  id: Int,
  pos: CapturePos,
  authorship: Option[Authorship] = None) extends AstNode

case class Infraspecies(
  id: Int,
  pos: CapturePos,
  rank: Option[Rank] = None,
  authorship: Option[Authorship]) extends AstNode

case class InfraspeciesGroup(
  id: Int,
  group: Seq[Infraspecies]) extends AstNode {

  val pos: CapturePos = CapturePos(group.head.pos.start, group.last.pos.end)
}

case class Year(
  id: Int,
  pos: CapturePos,
  alpha: Option[CapturePos] = None,
  approximate: Boolean = false) extends AstNode

case class AuthorWord(
  id: Int,
  pos: CapturePos) extends AstNode

case class Author(
  id: Int,
  words: Seq[AuthorWord],
  anon: Boolean = false,
  filius: Option[AuthorWord] = None) extends AstNode {

  val pos = {
    val end = filius.getOrElse(words.last).pos.end
    CapturePos(words.head.pos.start, end)
  }
}

case class AuthorsTeam(
  id: Int,
  authors: Seq[Author]) extends AstNode {

  val pos: CapturePos = {
    CapturePos(authors.sortBy { _.pos.start }.head.pos.start,
               authors.sortBy { _.pos.end }.head.pos.start)
  }
}

case class AuthorsGroup(
  id: Int,
  authors: AuthorsTeam,
  authorsEx: Option[AuthorsTeam] = None,
  year: Option[Year] = None) extends AstNode {

  val pos: CapturePos = {
    val nodes = Vector(authors.some, authorsEx, year).flatten
    CapturePos(nodes.sortBy { _.pos.start }.head.pos.start,
               nodes.sortBy { _.pos.end }.last.pos.end)
  }
}

case class Authorship(
  id: Int,
  authors: AuthorsGroup,
  combination: Option[AuthorsGroup] = None,
  inparenthesis: Boolean = false,
  private val basionymParsed: Boolean = false) extends AstNode {

  val pos: CapturePos = {
    val nodes = Vector(authors.some, combination).flatten
    CapturePos(nodes.sortBy { _.pos.start }.head.pos.start,
               nodes.sortBy { _.pos.end }.last.pos.end)
  }
  val basionym: Option[AuthorsGroup] = (basionymParsed || combination.isEmpty).option(authors)
}
