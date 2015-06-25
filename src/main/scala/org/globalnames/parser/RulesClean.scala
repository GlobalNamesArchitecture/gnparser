package org.globalnames.parser

import org.parboiled2._
import scala.collection.immutable.Seq
import CharPredicate.{Digit, Printable, Alpha}

trait RulesClean extends Parser {
  def sciName: Rule1[SciName] = rule {
    softSpace ~ sciName1 ~ softSpace ~ EOI ~> ((x: Node) =>
      SciName(
        verbatim = input.sliceString(0, input.length),
        normalized =  Some(x.normalized),
        canonical = Some(x.canonical),
        isParsed = true,
        parserRun = 1
      )
    )
  }

  def sciName1: Rule1[Node] = rule {
    sciName2 ~ (space ~ "sec." ~ oneOrMore(Printable)).?
  }

  def sciName2: Rule1[Node] = rule {
    sciName3 | sciName4
  }

  def sciName3: Rule1[Node] = rule {
    multinomial | multinomial1 | multinomial2
  }

  def sciName4: Rule1[Node] = rule {
    (nameAuthor | name)
  }

  def nameAuthor: Rule1[Node] = rule {
    name ~ space ~ authorship ~> ((w1: Node, w2: String) =>
      w1.copy(normalized = s"${w1.normalized} $w2")
    )
  }

  def authorship: Rule1[String] = rule {
    combinedAuthorship | basionymYearMisformed |
    basionymAuthorship | authorship1
  }

  def combinedAuthorship: Rule1[String] = rule {
    basionymAuthorship ~ space ~ authorship1 ~>
    ((bauth: String, auth: String) => s"$bauth $auth")
  }

  def basionymYearMisformed: Rule1[String] = rule {
    '(' ~ space ~ authors ~ space ~ ')' ~ (space ~ ',').? ~ space ~ year ~>
    ((a: String, y: String) => s"($a $y)")
  }

  def basionymAuthorship: Rule1[String] = rule {
    '(' ~ space ~ authorship1 ~ space ~ ')' ~>
    ((auth: String) => s"($auth)")
  }

  def authorship1: Rule1[String] = rule {
    authorsYear | authors
  }

  def authorsYear: Rule1[String] = rule {
    authorsYear1 | authorsYear2
  }

  def authorsYear1: Rule1[String] = rule {
    authors ~ space ~ ',' ~ space ~ year ~>
    ((a: String, y: String) => s"$a $y".toString)
  }

  def authorsYear2: Rule1[String] = rule {
    authors ~ space ~ year ~>
    ((a: String, y: String) => s"$a $y".toString)
  }

  def name: Rule1[Node] = rule {
    binomial | uninomial
  }

  def multinomial: Rule1[Node] = rule {
    (multinomial1 | multinomial2) ~ space ~ authorship ~>
    ((m: Node, a: String) => {
      Node(normalized = s"${m.normalized} $a",
           canonical = s"${m.canonical}")
    })
  }

  def multinomial1: Rule1[Node] = rule {
    (nameAuthor|name) ~ space ~ rank ~ space ~ word ~>
    ((b: Node, r: String, i: String) => {
      Node(normalized = s"${b.normalized} ${r.trim} ${Util.norm(i)}",
           canonical = s"${b.canonical} ${Util.norm(i)}")
    })
  }

  def multinomial2: Rule1[Node] = rule {
    name ~ space ~ word ~>
    ((b: Node, i: String) => {
      Node(normalized = s"${b.normalized} ${Util.norm(i)}",
           canonical = s"${b.canonical} ${Util.norm(i)}")
    })
  }

  def binomial: Rule1[Node] = rule {
    binomial2 | binomial1
  }

  def binomial1: Rule1[Node] = rule {
    uninomial ~ space ~ subGenus ~ space ~ word ~>
    ((g: Node, sg: Node, s: String) => {
      Node(normalized = s"${g.normalized} ${sg.normalized} ${Util.norm(s)}",
           canonical = s"${g.canonical} ${Util.norm(s)}")
    })
  }

  def binomial2: Rule1[Node] = rule {
    uninomial ~ space ~ word ~> ((w1: Node, w2: String) =>
      Node(normalized = s"${w1.normalized} ${Util.norm(w2)}",
           canonical = s"${w1.canonical} ${Util.norm(w2)}")
    )
  }

  def rank: Rule1[String] = rule {
    capture("morph." | "f.sp." | "B" | "ssp." | "ssp" | "mut." | "nat" |
     "nothosubsp." | "convar." | "pseudovar." | "sect." | "ser." | "var." |
     "subvar." |  "[var.]"  | "var" | "subsp." | "subsp" | "subf." |
     "race" | "forma." | "forma" | "fma." | "fma" | "form." |
     "form" | "fo." | "fo" | "f." | "α" | "ββ" | "β" | "γ" | "δ" |
     "ε" | "φ" | "θ" | "μ" | "a." | "b." | "c." | "d." | "e." | "g." |
     "k." | "****" | "**" | "*")
  }

  def subGenus: Rule1[Node] = rule {
    "(" ~ space ~ uninomial ~ space ~ ")" ~>
    ((x: Node) =>
        Node(normalized = s"(${x.normalized})", canonical = ""))
  }

  def uninomial: Rule1[Node] = rule {
    (abbrGenus | capWord | twoLetterGenera) ~> ((x: String) =>
      Node(normalized = Util.norm(x), canonical = Util.norm(x)))
  }

  def authors: Rule1[String] = rule {
    authors1 | author
  }

  def authors1: Rule1[String] = rule {
    author ~ space ~ authorSep ~ space ~ (authors | author) ~>
    ((au1: String, sep: String, au2: String) => {
        sep match {
          case "," => s"$au1, $au2"
          case x => s"$au1 $x $au2"
        }
      })
  }

  def authorSep: Rule1[String] = rule {
    authorComma | authorAnd | authorEx
  }

  def authorEx: Rule1[String] = rule {
    "ex" ~ push("ex")
  }

  def authorAnd: Rule1[String] = rule {
    ("&amp;" | "and" | "&" | "et") ~ push("&")
  }

  def authorComma: Rule1[String] = rule {
    "," ~ push(",")
  }

  def author: Rule1[String] = rule{
    oneOrMore(softSpace ~ authorWord) ~>
      ((au: Seq[String]) => au.map(_.trim).mkString(" "))
  }

  def authorWord: Rule1[String] = rule {
    authorWord1 | authorWord2 | authorWord4 | authorWord3 | authorPre
  }

  def authorWord1: Rule1[String] = rule {
    capture("Xu" |  "Xue" | "Xing")
  }

  def authorWord2: Rule1[String] = rule {
    capture("arg." | "et al.{?}" | "et al." | "et al")
  }

  def authorWord3: Rule1[String] = rule {
    capture(authCharUpper ~ zeroOrMore(authCharUpper | authCharLower)
      ~ '.'.?) ~>
      ((w: String) => Util.normAuth(w))
  }

  def authCharLower = rule {
    CharPredicate.LowerAlpha |
    CharPredicate("àáâãäåæçèéêëìíîïðñòóôõöøùúûüýÿāăąćĉčďđ") |
    CharPredicate("ēĕėęěğīĭİıĺľłńņňŏőœŕřśşšţťũūŭůűźżžſǎǔǧșțȳ")
  }

  def authCharUpper = rule {
    CharPredicate("ABCDEFGHIJKLMNOPQRSTUVWYZ") |
    CharPredicate("ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝ") |
    CharPredicate("ĆČĎİĶĹĺĽľŁłŅŌŐŒŘŚŜŞŠŸŹŻŽƒǾȘȚ")
  }

  def authorWord4: Rule1[String] = rule {
    authorWord3 ~ "-" ~ authorWord3 ~>
      ((au1: String, au2: String) => s"$au1-$au2" )
  }

  def authorPost: Rule1[String] = rule {
    capture("f."|"filius")
  }

  def authorPre: Rule1[String] = rule {
    capture("ab" | "af" | "bis" | "da" | "der" | "des" |
            "den" | "della" | "dela" | "de" | "di" | "du" |
            "la" | "ter" | "van" | "von")
  }

  def abbrGenus: Rule1[String] = rule {
    capture(upperChar ~ lowerChar.? ~ lowerChar.? ~ '.')
  }

  def capWord: Rule1[String] = rule {
    capture(upperChar ~ lowerChar ~ oneOrMore(lowerChar) ~ '?'.?)
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
    CharPredicate("abcdefghijklmnopqrstuvwxyz'ëæœſ")
  }

  def year: Rule1[String] = rule {
    yearWithParens | yearWithChar | yearNumber
  }

  def yearWithParens: Rule1[String] = rule {
    '(' ~ space ~ (yearWithChar | yearNumber) ~ space ~ ')'
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
    CharPredicate(" \t\r\n\f_щ")
  }
}
