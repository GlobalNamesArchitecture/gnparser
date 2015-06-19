package org.globalnames.parser

import org.parboiled2._
import CharPredicate.{Digit, Printable}

trait RulesClean extends Parser {
  def sciName: Rule1[SciName] = rule {
    softSpace ~ (nameAuthor | name) ~
      softSpace ~ EOI ~> ((x: Node) =>
      SciName(
        verbatim = input.sliceString(0, input.length),
        normalized =  Some(x.normalized),
        canonical = Some(x.canonical),
        isParsed = true,
        parserRun = 1
      )
    )
  }

  private def nameAuthor: Rule1[Node] = rule {
    name ~ space ~ authorship ~> ((w1: Node, w2: String) =>
      w1.copy(normalized = s"${w1.normalized} $w2")
    )
  }

  private def authorship: Rule1[String] = rule {
    authorYear | authorWord
  }

  private def authorYear: Rule1[String] = rule {
    authorWord ~ softSpace ~ year ~> ((a: String, y: String) =>
      s"$a $y".toString)
  }

  private def name: Rule1[Node] = rule {
    binomial | uninomial
  }

  private def binomial: Rule1[Node] = rule {
    capWord ~ space ~ word ~> ((w1: String, w2: String) =>
      Node(normalized = s"$w1 $w2", canonical = s"$w1 $w2")
    )
  }

  private def uninomial: Rule1[Node] = rule {
    capWord ~> ((x: String) =>
      Node(normalized = x, canonical = x))
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

  def lowerChar = rule {
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

