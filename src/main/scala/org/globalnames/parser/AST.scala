package org.globalnames.parser

import org.parboiled2.CapturePos

import scalaz.Scalaz._

trait AstNode {
  val pos: CapturePos
}

case class ScientificName(
  namesGroup: Option[NamesGroup] = None,
  isVirus: Boolean = false) {

  val isHybrid = namesGroup.map { ng => ng.name.size > 1 || ng.hybrid }
}

case class NamesGroup(
  name: Seq[Name],
  hybrid: Boolean = false,
  quality: Int = 1)

case class Name(
  uninomial: Uninomial,
  subgenus: Option[SubGenus] = None,
  species: Option[Species] = None,
  infraspecies: Option[InfraspeciesGroup] = None,
  comparison: Option[Comparison] = None,
  approximation: Option[Approximation] = None,
  ignored: Option[String] = None,
  quality: Int = 1) {

  val genus: Boolean = species.isDefined || approximation.isDefined
}

case class Comparison(pos: CapturePos) extends AstNode

case class Approximation(pos: CapturePos) extends AstNode

case class Rank(
  pos: CapturePos,
  typ: String) extends AstNode

case class Uninomial(
  pos: CapturePos,
  authorship: Option[Authorship] = None,
  rank: Option[Rank] = None,
  parent: Option[Uninomial] = None,
  quality: Int = 1) extends AstNode

case class UninomialWord(
  pos: CapturePos,
  quality: Int = 1) extends AstNode

case class SubGenus(
  subgenus: UninomialWord,
  quality: Int = 1) extends AstNode {

  val pos = subgenus.pos
}

case class Species(
  pos: CapturePos,
  authorship: Option[Authorship] = None,
  quality: Int = 1) extends AstNode

case class Infraspecies(
  pos: CapturePos,
  rank: Option[Rank] = None,
  authorship: Option[Authorship],
  quality: Int = 1) extends AstNode

case class InfraspeciesGroup(
  group: Seq[Infraspecies],
  quality: Int = 1)

case class Year(
  pos: CapturePos,
  alpha: Option[CapturePos] = None,
  quality: Int = 1) extends AstNode

case class Author(
  words: Seq[CapturePos],
  anon: Boolean = false,
  filius: Boolean = false,
  quality: Int = 1) extends AstNode {

  val pos = CapturePos(words.head.start, words.last.end)
}

case class AuthorsTeam(
  authors: Seq[Author],
  quality: Int = 1)

case class AuthorsGroup(
  authors: AuthorsTeam,
  authorsEx: Option[AuthorsTeam] = None,
  year: Option[Year] = None,
  quality: Int = 1)

case class Authorship(
  authors: AuthorsGroup,
  combination: Option[AuthorsGroup] = None,
  inparenthesis: Boolean = false,
  private val basionymParsed: Boolean = false,
  quality: Int = 1) {

  val basionym: Option[AuthorsGroup] = (basionymParsed || combination.isEmpty).option(authors)
}
