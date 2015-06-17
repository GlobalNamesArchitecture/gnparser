package org.globalnames.parser

import org.parboiled2._

object ParserClean {

  trait Line

  case class Comment() extends Line
  case class Blank() extends Line
  case class Name(verbatim: String) extends Line
}

class ParserClean(val input: ParserInput) extends Parser with StringBuilding {

  import CharPredicate.{Digit, Printable}
  import ParserClean._

  def line: Rule1[Line] = rule {
    noName | sciName
  }

  private def noName: Rule1[Line] = rule {
    blank | comment
  }

  private def sciName: Rule1[Line] = rule {
    softSpace ~ (nameAuthor | name) ~
      softSpace ~ EOI ~> (x => Name(x))
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

  private def blank: Rule1[Line] = rule {
    softSpace ~ EOI ~ push(Blank())
  }

  private def comment: Rule1[Line] = rule {
    softSpace ~ '#' ~ zeroOrMore(Printable) ~
      EOI ~ push(Comment())
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
