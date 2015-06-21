package org.globalnames.parser

import org.parboiled2._
import CharPredicate.{Digit, Printable, Alpha}

trait RulesClean extends Parser {
  def sciName: Rule1[SciName] = rule {
    softSpace ~ name1 ~ softSpace ~ EOI ~> ((x: Node) =>
      SciName(
        verbatim = input.sliceString(0, input.length),
        normalized =  Some(x.normalized),
        canonical = Some(x.canonical),
        isParsed = true,
        parserRun = 1
      )
    )
  }

  def name1: Rule1[Node] = rule {
    (nameAuthor | name)
  }

  def nameAuthor: Rule1[Node] = rule {
    name ~ space ~ authorship ~> ((w1: Node, w2: String) =>
      w1.copy(normalized = s"${w1.normalized} $w2")
    )
  }

  def authorship: Rule1[String] = rule {
    authorYear | authorWord
  }

  def authorYear: Rule1[String] = rule {
    authorWord ~ softSpace ~ ','.? ~ softSpace ~ year ~> ((a: String, y: String) =>
      s"$a $y".toString)
  }

  def name: Rule1[Node] = rule {
    binomial | uninomial
  }

  def binomial: Rule1[Node] = rule {
    capWord ~ space ~ word ~> ((w1: String, w2: String) =>
      Node(normalized = Util.norm(s"$w1 $w2"),
           canonical = Util.norm(s"$w1 $w2"))
    )
  }

  def uninomial: Rule1[Node] = rule {
    (twoLetterGenera | capWord) ~> ((x: String) =>
      Node(normalized = Util.norm(x), canonical = Util.norm(x)))
  }

  def authorWord: Rule1[String] = rule {
    capture((CharPredicate("ABCDEFGHIJKLMNOPQRSTUVWYZ") |
      CharPredicate("ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝ") |
      CharPredicate("ĆČĎİĶĹĺĽľŁłŅŌŐŒŘŚŜŞŠŸŹŻŽƒǾȘȚ"))
    ~
    zeroOrMore(CharPredicate.LowerAlpha |
      CharPredicate("-àáâãäåæçèéêëìíîïðñòóôõöøùúûüýÿāăąćĉčďđ") |
      CharPredicate("ēĕėęěğīĭİıĺľłńņňŏőœŕřśşšţťũūŭůűźżžſǎǔǧșțȳ"))
     ~ '.'.?)
  }

  def capWord: Rule1[String] = rule {
    capture(upperChar ~ lowerChar ~ oneOrMore(lowerChar))
  }

  def twoLetterGenera: Rule1[String] = rule {
    capture("Ca" | "Ea" | "Ge" | "Ia" | "Io" | "Io" | "Ix" | "Lo" | "Oa" |
      "Ra" | "Ty" | "Ua" | "Aa" | "Ja" | "Zu" | "La" | "Qu" | "As" | "Ba")
  }

  def word: Rule1[String] = rule {
    capture(lowerChar ~ oneOrMore(lowerChar))
  }

  def upperChar = rule {
    CharPredicate("ABCDEFGHIJKLMNOPQRSTUVWXYZËÆŒ")
  }

  def lowerChar = rule {
    CharPredicate("abcdefghijklmnopqrstuvwxyzëæœ")
  }

  def year: Rule1[String] = rule {
    yearWithParens | yearWithChar | yearNumber
  }


  def yearWithParens: Rule1[String] = rule {
    '(' ~ (yearWithChar | yearNumber) ~ ')'
  }

  def yearWithChar: Rule1[String] = rule {
    yearNumber ~ CharPredicate(Alpha)
  }

  def yearNumber: Rule1[String] = rule {
    capture(CharPredicate("12") ~
      CharPredicate("0789") ~ Digit ~ (Digit|'?') ~ '?'.?)
  }

  def softSpace = rule {
    zeroOrMore(spaceChars)
  }

  def space = rule {
    oneOrMore(spaceChars)
  }

  def spaceChars = rule {
    CharPredicate(" \t\r\n\f")
  }
}
