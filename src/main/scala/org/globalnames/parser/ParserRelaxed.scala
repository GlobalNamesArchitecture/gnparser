package org.globalnames.parser

import org.parboiled2.CharPredicate
import org.parboiled2.CharPredicate.{Digit, Alpha, LowerAlpha}

class ParserRelaxed extends ParserClean {

  // override val sciName: Rule1[SciName] = rule {
  //   softSpace ~ sciName1 ~ garbage.? ~ EOI ~> ((x: Node) =>
  //     SciName(
  //       verbatim = state.input.sliceString(0, state.input.length),
  //       normalized =  Some(x.normalized),
  //       canonical = Some(x.canonical),
  //       isParsed = true,
  //       isHybrid = x.hybrid
  //     )
  //   )
  // }
  //
  // val garbage = rule {
  //   space ~ oneOrMore(ANY)
  // }
  //
  // override val year: Rule1[Year] = rule {
  //   yearRange | yearApprox | yearWithParens | yearWithPage |
  //   yearWithDot | yearWithChar | yearNumber
  // }
  //
  // val yearRange: Rule1[Year] = rule {
  //   yearNumber ~ '-' ~ oneOrMore(Digit) ~ zeroOrMore("?" | Alpha) ~>
  //   ((y: Year) => y.copy(quality = 3))
  // }
  //
  // val yearWithDot: Rule1[Year] = rule {
  //   yearNumber ~ '.' ~> ((y: Year) => y.copy(quality = 3))
  // }
  //
  // val yearApprox: Rule1[Year] = rule {
  //   '[' ~ space ~ yearNumber ~ space ~ ']' ~>
  //    ((y: Year) => y.copy(quality = 3))
  // }
  //
  // val yearWithPage: Rule1[Year] = rule {
  //   (yearWithChar | yearNumber) ~ space ~ ':' ~ space ~ oneOrMore(Digit) ~>
  //   ((y: Year) => y.copy(quality = 3))
  // }
  //
  // override val lowerChar = CharPredicate(LowerAlpha ++ "'ëæœſàâåãäáçčéèíìïňññóòôøõöúùüŕřŗššşž")
}
