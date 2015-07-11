package org.globalnames.parser

import org.parboiled2._
import scala.collection.immutable.Seq
import CharPredicate.{Digit, Printable, Alpha}

class ParserClean(val input: ParserInput) extends Parser {
  def sciName: Rule1[SciName] = rule {
    softSpace ~ sciName1 ~ softSpace ~ EOI ~> ((x: Node) =>
      SciName(
        verbatim = input.sliceString(0, input.length),
        normalized =  Some(x.normalized),
        canonical = Some(x.canonical),
        isParsed = true,
        isHybrid = x.hybrid,
        parserRun = 1
      )
    )
  }

  def sciName1: Rule1[Node] = rule {
   hybridFormula | namedHybrid | approxName | sciName2
  }

  def sciName2: Rule1[Node] = rule {
    uninomialCombo | sciName3 | sciName4
  }

  def sciName3: Rule1[Node] = rule {
    sciName4 ~ space ~ (multinomial | multinomial1) ~>
    ((n: Node, m: Node) => {
      Node(normalized = s"${n.normalized} ${m.normalized}".trim,
           canonical = s"${n.canonical} ${m.canonical}".trim)
    })
  }

  def sciName4: Rule1[Node] = rule {
    name
  }

  def hybridFormula: Rule1[Node] = rule {
    sciName2 ~ space ~
    hybridChar ~ space ~
    (subspecies | species | sciName2).? ~>
    ((n1: Node, n2: Option[Node]) =>
        n2 match {
          case Some(x) =>
            Node(normalized = s"${n1.normalized} × ${x.normalized}",
              canonical = s"${n1.canonical} × ${x.canonical}", hybrid = true)
          case None =>
            Node(normalized = s"${n1.normalized} ×",
              canonical = s"${n1.canonical} ×", hybrid = true)
    })
  }

  def namedHybrid: Rule1[Node] = rule {
    hybridChar ~ softSpace ~ sciName2 ~>
    ((n: Node) =>
      Node(normalized = s"× ${n.normalized}",
        canonical = s"× ${n.canonical}", hybrid = true))
  }

  def authorship: Rule1[String] = rule {
    combinedAuthorship | basionymYearMisformed |
    basionymAuthorship | authorship1
  }
  def combinedAuthorship: Rule1[String] = rule {
    combinedAuthorship1 | combinedAuthorship2
  }

  def combinedAuthorship1: Rule1[String] = rule {
    basionymAuthorship ~ space ~ authorEx ~ space ~ authorship1 ~>
    ((bauth: String,ex: String, auth: String) => s"$bauth ex $auth")
  }

  def combinedAuthorship2: Rule1[String] = rule {
    basionymAuthorship ~ space ~ authorship1 ~>
    ((bauth: String, auth: String) => s"$bauth $auth")
  }

  def basionymYearMisformed: Rule1[String] = rule {
    '(' ~ space ~ authors ~ space ~ ')' ~ (space ~ ',').? ~ space ~ year ~>
    ((a: String, y: String) => s"($a $y)")
  }

  def basionymAuthorship: Rule1[String] = rule {
    basionymAuthorship1
  }

  def basionymAuthorship1: Rule1[String] = rule {
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
    binomial | uninomialAuth | uninomial
  }

  def subspecies: Rule1[Node] = rule {
    species ~ space ~ (multinomial | multinomial1) ~>
    ((s: Node, m: Node) =>
        Node(normalized = s"${s.normalized} ${m.normalized}",
          canonical = s"${s.canonical} ${m.canonical}"))
  }

  def multinomial: Rule1[Node] = rule {
    multinomial1 ~ space ~ (multinomial | multinomial1) ~>
    ((m1: Node, m2: Node) => {
      Node(normalized = s"${m1.normalized} ${m2.normalized}",
           canonical = s"${m1.canonical} ${m2.canonical}")
    })
  }

  def multinomial1: Rule1[Node] = rule {
    multinomial2 | multinomial3 | multinomial4
  }

  def multinomial2: Rule1[Node] = rule {
    (multinomial3 | multinomial4) ~ space ~ authorship ~>
    ((m: Node, a: String) => {
      Node(normalized = s"${m.normalized} $a",
           canonical = s"${m.canonical}")
    })
  }

  def multinomial3: Rule1[Node] = rule {
    rank ~ space ~ word ~>
    ((r: String, i: String) =>
       Node(normalized = s"$r ${Util.norm(i)}",
             canonical = s"${Util.norm(i)}")
    )
  }

  def multinomial4: Rule1[Node] = rule {
    word ~>
    ((i: String) => {
      Node(normalized = s"${Util.norm(i)}",
           canonical = s"${Util.norm(i)}")
    })
  }

  def binomial: Rule1[Node] = rule {
    binomial1 | binomial2 | binomial3
  }

  def binomial1: Rule1[Node] = rule {
    uninomial ~ space ~ comparison ~ (space ~ species).? ~>
    ((g: Node, _: String, s: Option[Node]) => {
      s match {
        case Some(x) =>
          Node(normalized = s"${g.normalized} cf. ${x.normalized}",
               canonical = s"${g.canonical} ${x.canonical}")
        case None => g
      }
    })
  }

  def binomial2: Rule1[Node] = rule {
    uninomial ~ space ~ subGenus ~ space ~ species ~>
    ((g: Node, sg: Node, s: Node) => {
      Node(normalized = s"${g.normalized} ${sg.normalized} ${s.normalized}",
           canonical = s"${g.canonical} ${s.canonical}")
    })
  }

  def binomial3: Rule1[Node] = rule {
    uninomial ~ space ~ species ~> ((w1: Node, w2: Node) =>
      Node(normalized = s"${w1.normalized} ${w2.normalized}",
           canonical = s"${w1.canonical} ${w2.canonical}")
    )
  }

  def species: Rule1[Node] = rule {
    species2 | species1
  }

  def species1: Rule1[Node] = rule {
    word ~>
    ((s: String) =>
        Node(normalized = s"${Util.norm(s)}", canonical = s"${Util.norm(s)}"))
  }

  def species2: Rule1[Node] = rule {
    species1 ~ space ~ authorship ~>
    ((s: Node, a: String) =>
        Node(normalized = s"${s.normalized} $a", canonical = s.normalized))
  }

  def comparison: Rule1[String] = rule {
    capture("cf." | "cf")
  }

  def approximation: Rule1[String] = rule {
    capture("spp." | "spp" | "sp.nr." | "sp. nr." | "nr." | "nr" | "sp.aff." |
      "sp. aff." | "sp." | "sp" | "species" |
      "aff." | "aff" | "monst." | "?")
  }

  def rankUninomial: Rule1[String] = rule {
    capture("sect." |"sect" |"subsect." |"subsect" |"trib." |
     "trib" |"subtrib." |"subtrib" |"ser." |"ser" |"subgen." |
     "subgen" |"fam." |"fam" |"subfam." |"subfam" |"supertrib." |
     "supertrib")
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

  def uninomialCombo: Rule1[Node] = rule {
    uninomialCombo1 | uninomialCombo2 | uninomialCombo3
  }

  def uninomialCombo1: Rule1[Node] = rule {
    (uninomialCombo2 | uninomialCombo3) ~ space ~ authorship ~>
    ((uc: Node, au: String) =>
        Node(normalized = s"${uc.normalized} $au",
          canonical = s"${uc.canonical}"))
  }

  def uninomialCombo2: Rule1[Node] = rule {
    uninomial ~ space ~ authorship ~ space ~ rankUninomial ~ space ~ uninomial ~>
    ((u1: Node, au: String, r: String, u2: Node) =>
        Node(normalized = s"${u1.normalized} $au $r ${u2.normalized}",
          canonical = s"${u2.canonical}"))
  }

  def uninomialCombo3: Rule1[Node] = rule {
    uninomial ~ space ~ rankUninomial ~ space ~ uninomial ~>
    ((u1: Node, r: String, u2: Node) =>
        Node(normalized = s"${u1.normalized} $r ${u2.normalized}",
          canonical = s"${u2.canonical}"))
  }

  def uninomial: Rule1[Node] = rule {
    (abbrGenus | capWord | twoLetterGenera) ~> ((x: String) =>
      Node(normalized = Util.norm(x), canonical = Util.norm(x)))
  }

  def uninomialAuth: Rule1[Node] = rule {
    uninomial ~ space ~ authorship ~> ((u: Node, a: String) =>
      Node(normalized = s"${u.normalized} $a", canonical = u.canonical))
  }

  def approxName: Rule1[Node] = rule {
    approxName1 | approxName2
  }

  def approxName1: Rule1[Node] = rule {
    uninomial ~ space ~ approximation ~ space ~ zeroOrMore(Printable|"щ") ~>
      ((g: Node, _: String) => g)
  }

  def approxName2: Rule1[Node] = rule {
    uninomial ~ space ~ word ~ space ~
    approximation ~ space ~ zeroOrMore(Printable|"щ") ~>
      ((g: Node, s: String, _: String) =>
          Node(normalized = s"${g.normalized} ${Util.norm(s)}",
               canonical = s"${g.canonical} ${Util.norm(s)}"))
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
    ("ex" | "in") ~ push("ex")
  }

  def authorAnd: Rule1[String] = rule {
    ("&amp;" | "and" | "&" | "et") ~ push("&")
  }

  def authorComma: Rule1[String] = rule {
    "," ~ push(",")
  }

  def author: Rule1[String] = rule {
    author1 | author2 | unknownAuthor
  }

  def author1: Rule1[String] = rule {
    author2 ~ space ~ filius ~> ((au: String) => s"$au f.")
  }

  def author2: Rule1[String] = rule {
    oneOrMore(authorWord).separatedBy(space) ~>
      ((au: Seq[String]) => au.mkString(" "))
  }

  def unknownAuthor: Rule1[String] = rule {
    capture("?" | "Яauct." | "Яauct" | "Яanon." | "Яanon" | "Яht." | "Яht" |
      "Яhort." | "Яhort") ~>
    ((a: String) => {
      if (a == "?") "?" else a.substring(1)
    })
  }

  def authorWord: Rule1[String] = rule {
    authorWord1 | authorWord2 | authorPre
  }

  def authorWord1: Rule1[String] = rule {
    capture("arg." | "et al.{?}" | "et al." | "et al")
  }

  def authorWord2: Rule1[String] = rule {
    capture(("d'").? ~ authCharUpper ~ zeroOrMore(authCharUpper | authCharLower)
      ~ '.'.?) ~>
      ((w: String) => Util.normAuthWord(w))
  }

  def authCharLower = rule {
    CharPredicate.LowerAlpha |
    CharPredicate("àáâãäåæçèéêëìíîïðñòóóôõöøùúûüýÿāăąćĉčďđ") |
    CharPredicate("'-ēĕėęěğīĭİıĺľłńņňŏőœŕřśşšţťũūŭůűźżžſǎǔǧșțȳß")
  }

  def authCharUpper = rule {
    CharPredicate("ABCDEFGHIJKLMNOPQRSTUVWXYZ") |
    CharPredicate("ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝ") |
    CharPredicate("ĆČĎİĶĹĺĽľŁłŅŌŐŒŘŚŜŞŠŸŹŻŽƒǾȘȚ�")
  }

  def filius = rule {
    "f." | "filius"
  }

  def authorPre: Rule1[String] = rule {
    capture("жab" | "жaf" | "жbis" | "жda" | "жder" | "жdes" |
            "жden" | "жdella" | "жdela" | "жde" | "жdi" | "жdu" |
            "жla" | "жter" | "жvan" | "жvon") ~>
    ((a: String) => a.substring(1))
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

  def hybridChar = rule { "×" | "*" }

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
