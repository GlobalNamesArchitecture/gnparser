package org.globalnames.parser


import org.parboiled2._
import CharPredicate.{Digit, Printable, Alpha}

class ParserRelaxed(override val input: ParserInput) extends
  ParserClean(input) {
  override def sciName: Rule1[SciName] = rule {
    softSpace ~ sciName1 ~ garbage.? ~ EOI ~> ((x: Node) =>
      SciName(
        verbatim = input.sliceString(0, input.length),
        normalized =  Some(x.normalized),
        canonical = Some(x.canonical),
        isParsed = true,
        isHybrid = x.hybrid,
        parserRun = 2
      )
    )
  }

  def garbage = rule {
    space ~ oneOrMore(ANY)
  }

  override def basionymAuthorship: Rule1[String] = rule {
    basionymAuthorship1 | basionymAuthorship2
  }

  def basionymAuthorship2: Rule1[String] = rule {
    '(' ~ space ~ '(' ~ space ~ authorship1 ~ space ~ ')' ~ space ~ ')' ~>
    ((auth: String) => s"($auth)")
  }

  override def year: Rule1[String] = rule {
    yearRange | yearApprox | yearWithParens | yearWithPage |
    yearWithDot | yearWithChar | yearNumber
  }

  def yearRange: Rule1[String] = rule {
    yearNumber ~ '-' ~ oneOrMore(Digit) ~ zeroOrMore("?" | Alpha)
  }

  def yearWithDot: Rule1[String] = rule {
    yearNumber ~ '.'
  }

  def yearApprox: Rule1[String] = rule {
    '[' ~ space ~ yearNumber ~ space ~ ']' ~>
     ((y: String) => s"($y)")
  }

  def yearWithPage: Rule1[String] = rule {
    yearNumber ~ ':' ~ space ~ oneOrMore(Digit)
  }

  val az = "abcdefghijklmnopqrstuvwxyz'ëæœſ-àâåãäáçčéèíìïňññóòôøõöúùüŕřŗššşž"

  override def lowerChar = rule {
    CharPredicate(az)
  }
}
