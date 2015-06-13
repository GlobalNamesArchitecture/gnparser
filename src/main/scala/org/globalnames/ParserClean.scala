package org.globalnames

import org.parboiled2._

/**
 * Created by dimus on 6/13/15.
 */

class ParserClean(val input: ParserInput) extends Parser {
  def InputLine = rule { ScientificName ~ EOI }

  def ScientificName: Rule1[String] = rule {
    Term ~ zeroOrMore(
      '+' ~ Term ~> ((_: Int) + _)
    | '-' ~ Term ~> ((_: Int) - _))
  }

  def Term = rule {
    Factor ~ zeroOrMore(
      '*' ~ Factor ~> ((_: Int) * _)
    | '/' ~ Factor ~> ((_: Int) / _))
  }

  def Factor = rule { Number | Parens }

  def Parens = rule { '(' ~ Expression ~ ')' }

  def Number = rule { capture(Digits) ~> (_.toInt) }

  def Digits = rule { oneOrMore(CharPredicate.Digit) }

  def LowChars = rule { oneOrMore(CharPredicate.LowerAlpha)}
}
