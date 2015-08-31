package org.globalnames.parser

import scalaz.{Name => _, _}
import Scalaz._
import org.parboiled2.CapturePos

trait AstNode {
  val pos: CapturePos
}

case class InputString(verbatim: String = "") {
  private lazy val UNESCAPE_HTML4 = new TrackingPositionsUnescapeHtml4Translator
  lazy val unescaped: String = {
    val unescaped = UNESCAPE_HTML4.translate(verbatim)
    val unjunk = ScientificNameParser.removeJunk(unescaped)
    ScientificNameParser.normalizeHybridChar(unjunk)
  }
}

case class ScientificName(
  input: InputString = InputString(),
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
  comparison: Option[String] = None,
  approximation: Option[String] = None,
  ignored: Option[String] = None,
  quality: Int = 1) {

  val genus: Boolean = species.isDefined || approximation.isDefined
}

case class Uninomial(
  str: String,
  authorship: Option[Authorship] = None,
  rank: Option[String] = None,
  parent: Option[Uninomial] = None,
  quality: Int = 1)

case class UninomialWord(
  str: String,
  quality: Int = 1)

case class SubGenus(
  subgenus: UninomialWord,
  quality: Int = 1)

case class Species(
  str: String,
  authorship: Option[Authorship] = None,
  quality: Int = 1)

case class Infraspecies(
  str: String,
  rank: Option[String] = None,
  authorship: Option[Authorship],
  quality: Int = 1)

case class InfraspeciesGroup(
  group: Seq[Infraspecies],
  quality: Int = 1)

case class Year(
  pos: CapturePos,
  alpha: Option[CapturePos] = None,
  quality: Int = 1) extends AstNode

case class Author(
  str: String,
  anon: Boolean = false,
  filius: Boolean = false,
  quality: Int = 1)

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
