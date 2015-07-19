package org.globalnames.parser

import org.parboiled2.SimpleParser
import scala.collection.immutable.Seq
import org.parboiled2.CharPredicate
import org.parboiled2.CharPredicate.{Digit, Printable, Alpha, LowerAlpha, UpperAlpha}

class ParserClean extends SimpleParser {
  val sciName: Rule1[SciName] = rule {
    softSpace ~ sciName1 ~ softSpace ~ EOI ~> ((x: Node) =>
      SciName(
        verbatim = state.input.sliceString(0, state.input.length),
        normalized =  Some(x.normalized),
        canonical = Some(x.canonical),
        isParsed = true,
        isHybrid = x.hybrid,
        parserRun = 1
      )
    )
  }

  val sciName1: Rule1[Node] = rule {
   hybridFormula | namedHybrid | approxName | sciName2
  }

  val sciName2: Rule1[Node] = rule {
    uninomialCombo | sciName3 | sciName4
  }

  val sciName3: Rule1[Node] = rule {
    sciName4 ~ space ~ (multinomial | multinomial1) ~>
    ((n: Node, m: Node) => {
      Node(normalized = s"${n.normalized} ${m.normalized}".trim,
           canonical = s"${n.canonical} ${m.canonical}".trim)
    })
  }

  val name: Rule1[Node] = rule {
    binomial | uninomialAuth | uninomial
  }

  val sciName4: Rule1[Node] = name

  val hybridFormula: Rule1[Node] = rule {
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

  val namedHybrid: Rule1[Node] = rule {
    hybridChar ~ softSpace ~ sciName2 ~>
    ((n: Node) =>
      Node(normalized = s"× ${n.normalized}",
        canonical = s"× ${n.canonical}", hybrid = true))
  }

  val subspecies: Rule1[Node] = rule {
    species ~ space ~ (multinomial | multinomial1) ~>
    ((s: Node, m: Node) =>
        Node(normalized = s"${s.normalized} ${m.normalized}",
          canonical = s"${s.canonical} ${m.canonical}"))
  }

  val multinomial: Rule1[Node] = rule {
    multinomial1 ~ space ~ (multinomial | multinomial1) ~>
    ((m1: Node, m2: Node) => {
      Node(normalized = s"${m1.normalized} ${m2.normalized}",
           canonical = s"${m1.canonical} ${m2.canonical}")
    })
  }

  val multinomial1: Rule1[Node] = rule {
    multinomial2 | multinomial3 | multinomial4
  }

  val multinomial2: Rule1[Node] = rule {
    (multinomial3 | multinomial4) ~ space ~ authorship ~>
    ((m: Node, a: String) => {
      Node(normalized = s"${m.normalized} $a",
           canonical = s"${m.canonical}")
    })
  }

  val multinomial3: Rule1[Node] = rule {
    rank ~ space ~ word ~>
    ((r: String, i: String) =>
       Node(normalized = s"$r ${Util.norm(i)}",
             canonical = s"${Util.norm(i)}")
    )
  }

  val multinomial4: Rule1[Node] = rule {
    word ~>
    ((i: String) => {
      Node(normalized = s"${Util.norm(i)}",
           canonical = s"${Util.norm(i)}")
    })
  }

  val binomial: Rule1[Node] = rule {
    binomial1 | binomial2 | binomial3
  }

  val binomial1: Rule1[Node] = rule {
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

  val binomial2: Rule1[Node] = rule {
    uninomial ~ space ~ subGenus ~ space ~ species ~>
    ((g: Node, sg: Node, s: Node) => {
      Node(normalized = s"${g.normalized} ${sg.normalized} ${s.normalized}",
           canonical = s"${g.canonical} ${s.canonical}")
    })
  }

  val binomial3: Rule1[Node] = rule {
    uninomial ~ space ~ species ~> ((w1: Node, w2: Node) =>
      Node(normalized = s"${w1.normalized} ${w2.normalized}",
           canonical = s"${w1.canonical} ${w2.canonical}")
    )
  }

  val species: Rule1[Node] = rule {
    species2 | species1
  }

  val species1: Rule1[Node] = rule {
    word ~>
    ((s: String) =>
        Node(normalized = s"$s", canonical = s"$s"))
  }

  val species2: Rule1[Node] = rule {
    species1 ~ space ~ authorship ~>
    ((s: Node, a: String) =>
        Node(normalized = s"${s.normalized} $a", canonical = s.normalized))
  }

  val comparison: Rule1[String] = rule {
    capture("cf." | "cf")
  }

  val approximation: Rule1[String] = rule {
    capture("spp." | "spp" | "sp.nr." | "sp. nr." | "nr." | "nr" | "sp.aff." |
      "sp. aff." | "sp." | "sp" | "species" |
      "aff." | "aff" | "monst." | "?")
  }

  val rankUninomial: Rule1[String] = rule {
    capture("sect." |"sect" |"subsect." |"subsect" |"trib." |
     "trib" |"subtrib." |"subtrib" |"ser." |"ser" |"subgen." |
     "subgen" |"fam." |"fam" |"subfam." |"subfam" |"supertrib." |
     "supertrib")
  }

  val rank: Rule1[String] = rule {
    capture("morph." | "f.sp." | "B" | "ssp." | "ssp" | "mut." | "nat" |
     "nothosubsp." | "convar." | "pseudovar." | "sect." | "ser." | "var." |
     "subvar." |  "[var.]"  | "var" | "subsp." | "subsp" | "subf." |
     "race" | "forma." | "forma" | "fma." | "fma" | "form." |
     "form" | "fo." | "fo" | "f." | "α" | "ββ" | "β" | "γ" | "δ" |
     "ε" | "φ" | "θ" | "μ" | "a." | "b." | "c." | "d." | "e." | "g." |
     "k." | "****" | "**" | "*")
  }

  val subGenus: Rule1[Node] = rule {
    "(" ~ space ~ uninomial ~ space ~ ")" ~>
    ((x: Node) =>
        Node(normalized = s"(${x.normalized})", canonical = ""))
  }

  val uninomialCombo: Rule1[Node] = rule {
    uninomialCombo1 | uninomialCombo2 | uninomialCombo3
  }

  val uninomialCombo1: Rule1[Node] = rule {
    (uninomialCombo2 | uninomialCombo3) ~ space ~ authorship ~>
    ((uc: Node, au: String) =>
        Node(normalized = s"${uc.normalized} $au",
          canonical = s"${uc.canonical}"))
  }

  val uninomialCombo2: Rule1[Node] = rule {
    uninomial ~ space ~ authorship ~ space ~ rankUninomial ~
      space ~ uninomial ~>
    ((u1: Node, au: String, r: String, u2: Node) =>
        Node(normalized = s"${u1.normalized} $au $r ${u2.normalized}",
          canonical = s"${u2.canonical}"))
  }

  val uninomialCombo3: Rule1[Node] = rule {
    uninomial ~ space ~ rankUninomial ~ space ~ uninomial ~>
    ((u1: Node, r: String, u2: Node) =>
        Node(normalized = s"${u1.normalized} $r ${u2.normalized}",
          canonical = s"${u2.canonical}"))
  }

  val uninomial: Rule1[Node] = rule {
    (abbrGenus | capWord | twoLetterGenera) ~> ((u: String) =>
      Node(normalized = u, canonical = u))
  }

  val uninomialAuth: Rule1[Node] = rule {
    uninomial ~ space ~ authorship ~> ((u: Node, a: String) =>
      Node(normalized = s"${u.normalized} $a", canonical = u.canonical))
  }

  val approxName: Rule1[Node] = rule {
    approxName1 | approxName2
  }

  val approxName1: Rule1[Node] = rule {
    uninomial ~ space ~ approximation ~ space ~ zeroOrMore(Printable|"щ") ~>
      ((g: Node, _: String) => g)
  }

  val approxName2: Rule1[Node] = rule {
    uninomial ~ space ~ word ~ space ~
    approximation ~ space ~ zeroOrMore(Printable|"щ") ~>
      ((g: Node, s: String, _: String) =>
          Node(normalized = s"${g.normalized} ${Util.norm(s)}",
               canonical = s"${g.canonical} ${Util.norm(s)}"))
  }

  val authorship: Rule1[String] = rule {
    combinedAuthorship | basionymYearMisformed |
    basionymAuthorship | authorship1
  }

  val combinedAuthorship: Rule1[String] = rule {
    combinedAuthorship1 | combinedAuthorship2
  }

  val combinedAuthorship1: Rule1[String] = rule {
    basionymAuthorship ~ space ~ authorEx ~ space ~ authorship1 ~>
    ((bauth: String,ex: String, auth: String) => s"$bauth ex $auth")
  }

  val combinedAuthorship2: Rule1[String] = rule {
    basionymAuthorship ~ space ~ authorship1 ~>
    ((bauth: String, auth: String) => s"$bauth $auth")
  }

  val basionymYearMisformed: Rule1[String] = rule {
    '(' ~ space ~ authors ~ space ~ ')' ~ (space ~ ',').? ~ space ~ year ~>
    ((a: String, y: String) => s"($a $y)")
  }

  val basionymAuthorship1: Rule1[String] = rule {
    '(' ~ space ~ authorship1 ~ space ~ ')' ~>
      ((auth: String) => s"($auth)")
  }

  val basionymAuthorship: Rule1[String] = basionymAuthorship1

  val authorship1: Rule1[String] = rule {
    authorsYear | authors
  }

  val authorsYear: Rule1[String] = rule {
    authors ~ space ~ (',' ~ space).? ~ year ~>
    ((a: String, y: String) => s"$a $y".toString)
  }


  val authors: Rule1[String] = rule {
    authors1 | author
  }

  val authors1: Rule1[String] = rule {
    author ~ space ~ authorSep ~ space ~ (authors | author) ~>
    ((au1: String, sep: String, au2: String) => {
        sep match {
          case "," => s"$au1, $au2"
          case x => s"$au1 $x $au2"
        }
      })
  }

  val authorSep: Rule1[String] = rule {
    authorComma | authorAnd | authorEx
  }

  val authorEx: Rule1[String] = rule {
    ("ex" | "in") ~ push("ex")
  }

  val authorAnd: Rule1[String] = rule {
    ("and" | "&" | "et") ~ push("&")
  }

  val authorComma: Rule1[String] = rule {
    capture(",")
  }

  val author: Rule1[String] = rule {
    author1 | author2 | unknownAuthor
  }

  val author1: Rule1[String] = rule {
    author2 ~ space ~ filius ~> ((au: String) => s"$au f.")
  }

  val author2: Rule1[String] = rule {
    oneOrMore(authorWord).separatedBy(space) ~>
      ((au: Seq[String]) => au.mkString(" "))
  }

  val unknownAuthor: Rule1[String] = rule {
    capture("?" |
            (("auct." | "auct" | "anon." | "anon" | "ht." | "ht" | "hort." | "hort") ~ &(spaceChars | EOI)))
  }

  val authorWord: Rule1[String] = rule {
    authorWord1 | authorWord2 | authorPre
  }

  val authorWord1: Rule1[String] = rule {
    capture("arg." | "et al.{?}" | "et al." | "et al")
  }

  val authorWord2: Rule1[String] = rule {
    capture("жd'".? ~ authCharUpper ~ zeroOrMore(authCharUpper | authCharLower)
      ~ '.'.?) ~>
      ((w: String) => Util.normAuthWord(w))
  }

  val authCharLower = CharPredicate(LowerAlpha ++
    "àáâãäåæçèéêëìíîïðñòóóôõöøùúûüýÿāăąćĉčďđ'-ēĕėęěğīĭİıĺľłńņňŏőœŕřśşšţťũūŭůűźżžſǎǔǧșțȳß")

  val authCharUpper = CharPredicate(UpperAlpha ++ "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝĆČĎİĶĹĺĽľŁłŅŌŐŒŘŚŜŞŠŸŹŻŽƒǾȘȚ�")

  val filius = rule {
    "f." | "filius"
  }

  val authorPre: Rule1[String] = rule {
    capture("жab" | "жaf" | "жbis" | "жda" | "жder" | "жdes" |
            "жden" | "жdella" | "жdela" | "жde" | "жdi" | "жdu" |
            "жla" | "жter" | "жvan" | "жvon" | "жd'") ~>
    ((a: String) => a.substring(1))
  }

  val abbrGenus: Rule1[String] = rule {
    capture(upperChar ~ lowerChar.? ~ lowerChar.? ~ '.')
  }

  val capWord: Rule1[String] = rule {
    capWord2 | capWord1
  }

  val capWord1: Rule1[String] = rule {
    capture(upperChar ~ lowerChar ~ oneOrMore(lowerChar) ~ '?'.?) ~>
    ((w: String) => Util.norm(w))
  }

  val capWord2: Rule1[String] = rule {
    capWord1 ~ "-" ~ word1 ~> ((w1: String, w2: String) => s"$w1-$w2")
  }

  val twoLetterGenera: Rule1[String] = rule {
    capture("Ca" | "Ea" | "Ge" | "Ia" | "Io" | "Io" | "Ix" | "Lo" | "Oa" |
      "Ra" | "Ty" | "Ua" | "Aa" | "Ja" | "Zu" | "La" | "Qu" | "As" | "Ba")
  }

  val word: Rule1[String] = rule {
    word2 | word1
  }

  val word1: Rule1[String] = rule {
    capture(lowerChar ~ oneOrMore(lowerChar)) ~> ((s: String) => Util.norm(s))
  }

  val word2: Rule1[String] = rule {
    word1 ~ "-" ~ word1 ~> ((s1: String, s2: String) => s"$s1-$s2")
  }

  val hybridChar = CharPredicate("×*")

  val upperChar = CharPredicate(UpperAlpha ++ "ËÆŒ")

  val lowerChar = CharPredicate(LowerAlpha ++ "'ëæœſ")

  val year: Rule1[String] = rule {
    yearWithParens | yearWithChar | yearNumber
  }

  val yearWithParens: Rule1[String] = rule {
    '(' ~ space ~ (yearWithChar | yearNumber) ~ space ~ ')'
  }

  val yearWithChar: Rule1[String] = rule {
    yearNumber ~ Alpha
  }

  val yearNumber: Rule1[String] = rule {
    capture(CharPredicate("12") ~ CharPredicate("0789") ~ Digit ~ (Digit|'?') ~ '?'.?)
  }

  val softSpace = rule {
    zeroOrMore(spaceChars)
  }

  val space = rule {
    oneOrMore(spaceChars)
  }

  val spaceChars = CharPredicate("　  \t\r\n\fщ_")
}
