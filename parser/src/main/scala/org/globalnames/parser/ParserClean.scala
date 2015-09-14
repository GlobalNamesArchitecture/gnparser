package org.globalnames.parser

import org.parboiled2.CharPredicate.{Alpha, Digit, LowerAlpha, UpperAlpha}
import org.parboiled2.{CapturePos, CharPredicate, SimpleParser}

import scalaz.Scalaz._

class ParserClean extends SimpleParser {
  val sciName: Rule1[ScientificName] = rule {
    softSpace ~ sciName1 ~ anyChars ~ EOI ~>
      ((n: NamesGroup, g: String) => ScientificName(namesGroup = n.some))
  }

  val sciName1: Rule1[NamesGroup] = rule {
    hybridFormula | namedHybrid | approxName | sciName2
  }

  val sciName2: Rule1[NamesGroup] = rule {
    name ~> ((n: Name) => NamesGroup(Vector(n)))
  }

  val hybridFormula: Rule1[NamesGroup] = rule {
    hybridFormula1 | hybridFormula2
  }

  val hybridFormula1: Rule1[NamesGroup] = rule {
    name ~ space ~ hybridChar ~ space ~
    species ~ (space ~ infraspeciesGroup).? ~>
    ((n: Name, hc: HybridChar, s: Species, i: Option[InfraspeciesGroup]) =>
      NamesGroup(
        name = Vector(n, Name(uninomial = Uninomial(CapturePos(0, 1)),
                              species = s.some, infraspecies = i)),
        hybrid = hc.some,
        quality = 3))
  }

  val hybridFormula2: Rule1[NamesGroup] = rule {
    name ~ space ~ hybridChar ~ (space ~ name).? ~>
    ((n1: Name, hc: HybridChar, n2: Option[Name]) =>
      n2 match {
        case None    => NamesGroup(name = Vector(n1), hybrid = hc.some, quality = 3)
        case Some(n) => NamesGroup(name = Vector(n1, n), hybrid = hc.some)
      }
    )
  }

  val namedHybrid: Rule1[NamesGroup] = rule {
    hybridChar ~ softSpace ~ name ~>
    ((hc: HybridChar, n: Name) => NamesGroup(Vector(n), hybrid = hc.some))
  }

  val name: Rule1[Name] = rule {
    name2 | name3 | name1
  }

  val name1: Rule1[Name] = rule {
    (uninomialCombo | uninomial) ~> ((u: Uninomial) => Name(u))
  }

  val name2: Rule1[Name] = rule {
    uninomialWord ~ space ~ comparison ~ (space ~ species).? ~>
    ((u: UninomialWord, c: Comparison, s: Option[Species]) =>
      Name(uninomial = Uninomial(u.pos, quality = u.quality),
           species = s, comparison = c.some, quality = 3))
  }

  val name3: Rule1[Name] = rule {
    uninomialWord ~ (softSpace ~ subGenus).? ~ softSpace ~
    species ~ (space ~ infraspeciesGroup).? ~>
    ((uninomialWord: UninomialWord, maybeSubGenus: Option[SubGenus], species: Species,
      maybeInfraspeciesGroup: Option[InfraspeciesGroup]) =>
      Name(Uninomial(uninomialWord.pos, quality = uninomialWord.quality),
           maybeSubGenus,
           species = species.some,
           infraspecies = maybeInfraspeciesGroup))
  }

  val infraspeciesGroup: Rule1[InfraspeciesGroup] = rule {
    oneOrMore(infraspecies).separatedBy(space) ~>
    ((inf: Seq[Infraspecies]) => InfraspeciesGroup(inf))
  }

  val infraspecies: Rule1[Infraspecies] = rule {
    (rank ~ softSpace).? ~ word ~ (space ~ authorship).? ~>
    ((r: Option[Rank], w: CapturePos, a: Option[Authorship]) => Infraspecies(w, r, a))
  }

  val species: Rule1[Species] = rule {
    word ~ (softSpace ~ authorship).? ~ &(spaceCharsEOI ++ "(,:") ~>
      ((s: CapturePos, a: Option[Authorship]) => Species(s, a))
  }

  val comparison: Rule1[Comparison] = rule {
    capturePos("cf" ~ '.'.?) ~> ((p: CapturePos) => Comparison(p))
  }

  val approximation: Rule1[Approximation] = rule {
    capturePos("sp.nr." | "sp. nr." | "sp.aff." | "sp. aff." | "monst." | "?" |
      (("spp" | "nr" | "sp" | "aff" | "species") ~ (&(spaceCharsEOI) | '.'))) ~>
      ((p: CapturePos) => Approximation(p))
  }

  val rankUninomial: Rule1[Rank] = rule {
    capturePos(("sect" | "subsect" | "trib" | "subtrib" | "ser" | "subgen" |
      "fam" | "subfam" | "supertrib") ~ '.'.?) ~>
      ((p: CapturePos) => Rank(p, state.input.sliceString(p.start, p.end)))
  }

  val rank: Rule1[Rank] = rule {
    rankForma | rankVar | rankSsp | rankOther
  }

  val rankOther: Rule1[Rank] = rule {
    capturePos("morph." | "f.sp." | "B" | "mut." | "nat" |
     "nothosubsp." | "convar." | "pseudovar." | "sect." | "ser." |
     "subvar." | "subf." | "race" | "α" | "ββ" | "β" | "γ" | "δ" |
     "ε" | "φ" | "θ" | "μ" | "a." | "b." | "c." | "d." | "e." | "g." |
     "k." | "****" | "**" | "*") ~ &(spaceCharsEOI) ~>
      ((p: CapturePos) => Rank(p, state.input.sliceString(p.start, p.end)))
  }

  val rankVar: Rule1[Rank] = rule {
    capturePos("[var.]" | ("var" ~ (&(spaceCharsEOI) | '.'))) ~>
      ((p: CapturePos) => Rank(p, "var."))
  }

  val rankForma: Rule1[Rank] = rule {
    capturePos(("forma"  | "fma" | "form" | "fo" | "f") ~
      (&(spaceCharsEOI) | '.')) ~> ((p: CapturePos) => Rank(p, "form"))
  }

  val rankSsp: Rule1[Rank] = rule {
    capturePos(("ssp" | "subsp") ~ (&(spaceCharsEOI) | '.')) ~>
      ((p: CapturePos) => Rank(p, "ssp."))
  }

  val subGenus: Rule1[SubGenus] = rule {
    '(' ~ softSpace ~ uninomialWord ~ softSpace ~ ')' ~> {
      (u: UninomialWord) =>
        val size = u.pos.end - u.pos.start
        val lastCh = state.input.charAt(u.pos.end - 1)
        if (size < 2 || lastCh == '.') SubGenus(u, 3)
        else SubGenus(u)
    }
  }

  val uninomialCombo: Rule1[Uninomial] = rule {
    uninomialCombo1 | uninomialCombo2
  }

  val uninomialCombo1: Rule1[Uninomial] = rule {
    uninomialWord ~ softSpace ~ subGenus ~ softSpace ~ authorship.? ~>
    ((uw: UninomialWord, sg: SubGenus, a: Option[Authorship]) =>
      Uninomial(sg.pos, a, Rank(CapturePos.empty, typ = "subgen.").some,
                Uninomial(uw.pos).some))
  }

  val uninomialCombo2: Rule1[Uninomial] = rule {
    (uninomial ~ softSpace ~ rankUninomial ~ softSpace ~ uninomial) ~>
    ((u1: Uninomial, r: Rank, u2: Uninomial) =>
      u2.copy(rank = r.some, parent = u1.some))
  }

  val uninomial: Rule1[Uninomial] = rule {
    uninomialWord ~ (space ~ authorship).? ~>
    ((u: UninomialWord, authorship: Option[Authorship]) => Uninomial(u.pos, authorship))
  }

  val uninomialWord: Rule1[UninomialWord] = rule {
    abbrGenus | capWord | twoLetterGenera
  }

  val abbrGenus: Rule1[UninomialWord] = rule {
    capturePos(upperChar ~ lowerChar.? ~ lowerChar.? ~ '.') ~>
      ((wp: CapturePos) => UninomialWord(wp, 3))
  }

  val capWord: Rule1[UninomialWord] = rule {
    capWord2 | capWord1
  }

  val capWord1: Rule1[UninomialWord] = rule {
    capturePos(upperChar ~ lowerChar ~ oneOrMore(lowerChar) ~ '?'.?) ~> {
      (p: CapturePos) =>
        if (state.input.charAt(p.end - 1) == '?') UninomialWord(p, 3)
        else UninomialWord(p)
    }
  }

  val capWord2: Rule1[UninomialWord] = rule {
    capWord1 ~ '-' ~ word1 ~ &(spaceCharsEOI ++ '(') ~> {
      (uw: UninomialWord, wPos: CapturePos) =>
        uw.copy(pos = CapturePos(uw.pos.start, wPos.end))
    }
  }

  val twoLetterGenera: Rule1[UninomialWord] = rule {
    capturePos("Ca" | "Ea" | "Ge" | "Ia" | "Io" | "Io" | "Ix" | "Lo" | "Oa" |
      "Ra" | "Ty" | "Ua" | "Aa" | "Ja" | "Zu" | "La" | "Qu" | "As" | "Ba") ~>
    ((p: CapturePos) => UninomialWord(p))
  }

  val word: Rule1[CapturePos] = rule {
    (word2 | word1) ~ &(spaceCharsEOI ++ '(')
  }

  val word1: Rule1[CapturePos] = rule {
    capturePos(lowerChar ~ oneOrMore(lowerChar))
  }

  val word2: Rule1[CapturePos] = rule {
    word1 ~ '-' ~ word1 ~> {
      (p1: CapturePos, p2: CapturePos) => CapturePos(p1.start, p2.end)
    }
  }

  val hybridChar: Rule1[HybridChar] = rule { capturePos('×') ~> HybridChar }

  val upperChar = CharPredicate(UpperAlpha ++ "ËÆŒ")

  val lowerChar = CharPredicate(LowerAlpha ++ "ëæœſàâåãäáçčéèíìïňññóòôøõöúùüŕřŗššşž'")

  val anyChars: Rule1[String] = rule { capture(zeroOrMore(ANY)) }

  val approxName: Rule1[NamesGroup] = rule {
    (approxName1 | approxName2) ~>
    ((n: Name) =>
     NamesGroup(name = Vector(n), quality = 3))
  }

  val approxName1: Rule1[Name] = rule {
    uninomial ~ space ~ approximation ~ softSpace ~ anyChars ~>
      ((u: Uninomial, appr: Approximation, ign: String) =>
          Name(uninomial = u, approximation = appr.some,
               ignored = ign.some, quality = 3))
  }

  val approxName2: Rule1[Name] = rule {
    (uninomial ~ space ~ word ~ space ~ approximation ~ space ~ anyChars) ~>
      ((u: Uninomial, s: CapturePos, appr: Approximation, ign: String) =>
        Name(uninomial = u,
             species = Species(s).some,
             approximation = appr.some,
             ignored = ign.some,
             quality = 3)
      )
  }

  val authorship: Rule1[Authorship] = rule {
    combinedAuthorship | basionymYearMisformed |
    basionymAuthorship | authorship1
  }

  val combinedAuthorship: Rule1[Authorship] = rule {
    combinedAuthorship1 | combinedAuthorship2
  }

  val combinedAuthorship1: Rule1[Authorship] = rule {
    basionymAuthorship ~ authorEx ~ authorship1 ~>
    ((bau: Authorship, exau: Authorship) =>
        bau.copy(authors = bau.authors.copy(
          authorsEx = exau.authors.authors.some), quality = 3))
  }

  val combinedAuthorship2: Rule1[Authorship] = rule {
    basionymAuthorship ~ softSpace ~ authorship1 ~>
    ((bau: Authorship, cau: Authorship) =>
      bau.copy(combination = cau.authors.some, basionymParsed = true, quality = 1))
  }

  val basionymYearMisformed: Rule1[Authorship] = rule {
    '(' ~ softSpace ~ authorsGroup ~ softSpace ~ ')' ~ (softSpace ~ ',').? ~ softSpace ~ year ~>
    ((a: AuthorsGroup, y: Year) => Authorship(authors = a.copy(year = y.some), inparenthesis = true,
                                              basionymParsed = true, quality = 3))
  }

  val basionymAuthorship: Rule1[Authorship] = rule {
    basionymAuthorship1 | basionymAuthorship2
  }

  val basionymAuthorship1: Rule1[Authorship] = rule {
    '(' ~ softSpace ~ authorship1 ~ softSpace ~ ')' ~>
    ((a: Authorship) => a.copy(basionymParsed = true, inparenthesis = true, quality = 2))
  }

  val basionymAuthorship2: Rule1[Authorship] = rule {
    '(' ~ softSpace ~ '(' ~ softSpace ~ authorship1 ~ softSpace ~ ')' ~ softSpace ~ ')' ~>
    ((a: Authorship) => a.copy(basionymParsed = true, inparenthesis = true, quality = 3))
  }

  val authorship1: Rule1[Authorship] = rule {
    (authorsYear | authorsGroup) ~>
    ((a: AuthorsGroup) => Authorship(a))
  }

  val authorsYear: Rule1[AuthorsGroup] = rule {
    authorsGroup ~ softSpace ~ (',' ~ softSpace).? ~ year ~>
    ((a: AuthorsGroup, y: Year) => a.copy(year = y.some))
  }

  val authorsGroup: Rule1[AuthorsGroup] = rule {
    authorsTeam ~ (authorEx ~ authorsTeam).? ~>
    ((a: AuthorsTeam, exAu: Option[AuthorsTeam]) =>
      AuthorsGroup(a, exAu))
  }

  val authorsTeam: Rule1[AuthorsTeam] = rule {
    oneOrMore(author).separatedBy(authorSep) ~>
    ((a: Seq[Author]) => AuthorsTeam(a))
  }

  val authorSep = rule {
    softSpace ~ ("," | "&" | "and" | "et") ~ softSpace
  }

  val authorEx = rule {
    space ~ ("ex" | "in") ~ space
  }

  val author: Rule1[Author] = rule {
    author1 | author2 | unknownAuthor
  }

  val author1: Rule1[Author] = rule {
    author2 ~ softSpace ~ filius ~> { (au: Author, filiusPos: CapturePos) =>
      au.copy(filius = filiusPos.some)
    }
  }

  val author2: Rule1[Author] = rule {
    oneOrMore(authorWord).separatedBy(softSpace) ~>
      ((au: Seq[CapturePos]) => Author(au))
  }

  val unknownAuthor: Rule1[Author] = rule {
    capturePos("?" |
            (("auct" | "anon" | "ht" | "hort") ~ (&(spaceCharsEOI) | '.'))) ~>
    ((auth: CapturePos) => Author(Seq(auth), anon = true, quality = 3))
  }

  val authorWord: Rule1[CapturePos] = rule {
    authorWord1 | authorWord2 | authorPre
  }

  val authorWord1: Rule1[CapturePos] = rule {
    capturePos("arg." | "et al.{?}" | "et al." | "et al")
  }

  val authorWord2: Rule1[CapturePos] = rule {
    capturePos("d'".? ~ authCharUpper ~ zeroOrMore(authCharUpper | authCharLower) ~ '.'.?)
  }

  val authCharLower = CharPredicate(LowerAlpha ++
    "àáâãäåæçèéêëìíîïðñòóóôõöøùúûüýÿāăąćĉčďđ'-ēĕėęěğīĭİıĺľłńņňŏőœŕřśşšţťũūŭůűźżžſǎǔǧșțȳß")

  val authCharUpper = CharPredicate(UpperAlpha ++ "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝĆČĎİĶĹĺĽľŁłŅŌŐŒŘŚŜŞŠŸŹŻŽƒǾȘȚ�")

  val filius: Rule1[CapturePos] = rule {
    capturePos("f." | "filius")
  }

  val authorPre: Rule1[CapturePos] = rule {
    capturePos("ab" | "af" | "bis" | "da" | "der" | "des" |
            "den" | "della" | "dela" | "de" | "di" | "du" |
            "la" | "ter" | "van" | "von" | "d'") ~ &(spaceCharsEOI)
  }

  val year: Rule1[Year] = rule {
    yearRange | yearApprox | yearWithParens | yearWithPage |
    yearWithDot | yearWithChar | yearNumber
  }

  val yearRange: Rule1[Year] = rule {
    yearNumber ~ '-' ~ oneOrMore(Digit) ~ zeroOrMore(Alpha ++ "?") ~>
    ((y: Year) => y.copy(approximate = true, quality = 3))
  }

  val yearWithDot: Rule1[Year] = rule {
    yearNumber ~ '.' ~> ((y: Year) => y.copy(quality = 3))
  }

  val yearApprox: Rule1[Year] = rule {
    '[' ~ softSpace ~ yearNumber ~ softSpace ~ ']' ~>
      ((y: Year) => y.copy(approximate = true, quality = 3))
  }

  val yearWithPage: Rule1[Year] = rule {
    (yearWithChar | yearNumber) ~ space ~ ':' ~ space ~ oneOrMore(Digit) ~>
    ((y: Year) => y.copy(quality = 3))
  }

  val yearWithParens: Rule1[Year] = rule {
    '(' ~ softSpace ~ (yearWithChar | yearNumber) ~ softSpace ~ ')' ~>
    ((y: Year) => y.copy(approximate = true, quality = 2))
  }

  val yearWithChar: Rule1[Year] = rule {
    yearNumber ~ capturePos(Alpha) ~> { (y: Year, pos: CapturePos) =>
      y.copy(alpha = pos.some, quality = 2)
    }
  }

  val yearNumber: Rule1[Year] = rule {
    capturePos(CharPredicate("12") ~ CharPredicate("0789") ~ Digit ~
      (Digit|'?') ~ '?'.?) ~> { (yPos: CapturePos) =>
        if (state.input.charAt(yPos.end - 1) == '?')
          Year(yPos, approximate = true, quality = 3)
        else Year(yPos)
    }
  }

  val softSpace = rule {
    zeroOrMore(spaceChars)
  }

  val space = rule {
    oneOrMore(spaceChars)
  }

  val spaceChars = CharPredicate("　  \t\r\n\f_")

  val spaceCharsEOI = spaceChars ++ EOI
}
