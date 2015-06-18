package org.globalnames.parser

import org.parboiled2._

object ParserClean {

}

class ParserClean(val input: ParserInput) extends Parser with StringBuilding {

  import CharPredicate.{Digit, Printable}
  import ParserClean._

  def sciName: Rule1[SciName] = rule {
    softSpace ~ (nameAuthor | name) ~
      softSpace ~ EOI ~> ((x: String) =>
      SciName(
        verbatim = input.toString,
        normalized =  Some(x),
        isParsed = true
      )
    )
  }

  private def authorship: Rule1[String] = rule {
    authorYear | authorWord
  }

  private def authorYear: Rule1[String] = rule {
    authorWord ~ softSpace ~ year ~> ((a: String, y: String) =>
      s"$a $y".toString)
  }

  private def nameAuthor: Rule1[String] = rule {
    name ~ space ~ authorship ~> ((w1: String, w2: String) =>
      s"$w1 $w2".toString)
  }

  private def name: Rule1[String] = rule {
    binomial | uninomial
  }

  private def binomial: Rule1[String] = rule {
    capWord ~ space ~ word ~> ((w1: String, w2: String) =>
      s"$w1 $w2".toString)
  }

  private def uninomial: Rule1[String] = rule {
    capWord
  }

  private def authorWord: Rule1[String] = rule {
    capture(CharPredicate.UpperAlpha ~
      zeroOrMore(CharPredicate.LowerAlpha) ~ '.'.?)
  }

  private def capWord: Rule1[String] = rule {
    capture(upperChar ~ oneOrMore(lowerChar))
  }

  private def word: Rule1[String] = rule {
    capture(lowerChar ~ oneOrMore(lowerChar))
  }

  private def upperChar = rule {
    CharPredicate("ABCDEFGHIJKLMNOPQRSTUVWXYZËÆŒ")
  }

  private def lowerChar = rule {
    CharPredicate("abcdefghijklmnopqrstuvwxyzëæœ")
  }

  private def year: Rule1[String] = rule {
    capture(CharPredicate("12") ~
      CharPredicate("0789") ~ Digit ~ Digit)
  }

  private def softSpace = rule {
    zeroOrMore(spaceChars)
  }

  private def space = rule {
    oneOrMore(spaceChars)
  }

  private def spaceChars = rule {
    CharPredicate(" \t\r\n\f")
  }
}
