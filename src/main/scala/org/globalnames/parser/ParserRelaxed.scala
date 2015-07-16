package org.globalnames.parser

import org.parboiled2.CharPredicate
import org.parboiled2.CharPredicate.{Digit, Alpha, LowerAlpha}

class ParserRelaxed extends ParserClean {

  override val sciName: Rule1[SciName] = rule {
    softSpace ~ sciName1 ~ garbage.? ~ EOI ~> ((x: Node) =>
      SciName(
        verbatim = state.input.sliceString(0, state.input.length),
        normalized =  Some(x.normalized),
        canonical = Some(x.canonical),
        isParsed = true,
        isHybrid = x.hybrid,
        parserRun = 2
      )
    )
  }

  val garbage = rule {
    space ~ oneOrMore(ANY)
  }

  override val basionymAuthorship: Rule1[String] = rule {
    basionymAuthorship1 | basionymAuthorship2
  }

  val basionymAuthorship2: Rule1[String] = rule {
    '(' ~ space ~ '(' ~ space ~ authorship1 ~ space ~ ')' ~ space ~ ')' ~>
    ((auth: String) => s"($auth)")
  }

  override val year: Rule1[String] = rule {
    yearRange | yearApprox | yearWithParens | yearWithPage |
    yearWithDot | yearWithChar | yearNumber
  }

  val yearRange: Rule1[String] = rule {
    yearNumber ~ '-' ~ oneOrMore(Digit) ~ zeroOrMore("?" | Alpha)
  }

  val yearWithDot: Rule1[String] = rule {
    yearNumber ~ '.'
  }

  val yearApprox: Rule1[String] = rule {
    '[' ~ space ~ yearNumber ~ space ~ ']' ~>
     ((y: String) => s"($y)")
  }

  val yearWithPage: Rule1[String] = rule {
    (yearWithChar | yearNumber) ~ space ~ ':' ~ space ~ oneOrMore(Digit)
  }

  override val lowerChar = CharPredicate(LowerAlpha ++ "'ëæœſàâåãäáçčéèíìïňññóòôøõöúùüŕřŗššşž")
}
