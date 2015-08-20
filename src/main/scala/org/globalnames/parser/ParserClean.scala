package org.globalnames.parser

import org.parboiled2.SimpleParser
import scala.collection.immutable.Seq
import org.parboiled2.CharPredicate
import org.parboiled2.CharPredicate.{Digit, Printable, Alpha, LowerAlpha,
                                     UpperAlpha}

class ParserClean extends SimpleParser {
  val sciName: Rule1[ScientificName] = rule {
    softSpace ~ sciName1 ~ anyChars ~ EOI ~>
      ((n: NamesGroup, g: String) => ScientificName(namesGroup = Some(n)))
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
    ((n: Name, s: Species, i: Option[InfraspeciesGroup]) =>
      NamesGroup(
        name = Vector(n, Name(uninomial = Uninomial(""), species = Some(s), infraspecies = i)),
        hybrid = true,
        quality = 3))
  }

  val hybridFormula2: Rule1[NamesGroup] = rule {
    name ~ space ~ hybridChar ~ (space ~ name).? ~>
    ((n1: Name, n2: Option[Name]) =>
      n2 match {
        case None    => NamesGroup(name = Vector(n1), hybrid = true, quality = 3)
        case Some(n) => NamesGroup(name = Vector(n1, n), hybrid = true)
      }
    )
  }

  val namedHybrid: Rule1[NamesGroup] = rule {
    hybridChar ~ softSpace ~ name ~>
    ((n: Name) => NamesGroup(Vector(n), hybrid = true))
  }

  val name: Rule1[Name] = rule {
    name2 | name3 | name1
  }

  val name1: Rule1[Name] = rule {
    (uninomialCombo | uninomial) ~> ((u: Uninomial) => Name(u))
  }

  val name2: Rule1[Name] = rule {
    uninomialWord ~ space ~ comparison ~ (space ~ species).? ~>
    ((u: UninomialWord, s: Option[Species]) =>
      Name(uninomial = Uninomial(u.str, quality = u.quality),
           species = s, comparison = Some("cf."), quality = 3))
  }

  val name3: Rule1[Name] = rule {
    uninomialWord ~ (softSpace ~ subGenus).? ~ softSpace ~
    species ~ (space ~ infraspeciesGroup).? ~>
    ((uninomialWord: UninomialWord, maybeSubGenus: Option[SubGenus], species: Species,
      maybeInfraspeciesGroup: Option[InfraspeciesGroup]) =>
      Name(uninomial = Uninomial(str = uninomialWord.str, quality = uninomialWord.quality),
           maybeSubGenus,
           species = Some(species),
           infraspecies = maybeInfraspeciesGroup))
  }

  val infraspeciesGroup: Rule1[InfraspeciesGroup] = rule {
    oneOrMore(infraspecies).separatedBy(space) ~>
    ((inf: Seq[Infraspecies]) => InfraspeciesGroup(inf))
  }

  val infraspecies: Rule1[Infraspecies] = rule {
    (rank ~ softSpace).? ~ word ~ (space ~ authorship).? ~>
    ((r: Option[String], w: String, a: Option[Authorship]) =>
        Infraspecies(w, r, a))
  }

  val species: Rule1[Species] = rule {
    word ~ (softSpace ~ authorship).? ~ &(spaceCharsEOI ++ "(,:") ~>
    ((s: String, a: Option[Authorship]) =>
      Species(s, a))
  }

  val comparison = rule {
    "cf" ~ '.'.?
  }

  val approximation: Rule1[String] = rule {
    capture("sp.nr." | "sp. nr." | "sp.aff." | "sp. aff." | "monst." | "?" |
      (("spp" | "nr" | "sp" | "aff" | "species") ~ (&(spaceCharsEOI) | '.')))
  }

  val rankUninomial: Rule1[String] = rule {
    capture(("sect" | "subsect" | "trib" | "subtrib" | "ser" | "subgen" |
      "fam" | "subfam" | "supertrib") ~ '.'.?)
  }

  val rank: Rule1[String] = rule {
    rankForma | rankVar | rankSsp | rankOther
  }

  val rankOther: Rule1[String] = rule {
    capture("morph." | "f.sp." | "B" | "mut." | "nat" |
     "nothosubsp." | "convar." | "pseudovar." | "sect." | "ser." |
     "subvar." | "subf." | "race" | "α" | "ββ" | "β" | "γ" | "δ" |
     "ε" | "φ" | "θ" | "μ" | "a." | "b." | "c." | "d." | "e." | "g." |
     "k." | "****" | "**" | "*") ~ &(spaceCharsEOI)
  }

  val rankVar: Rule1[String] = rule {
    ("[var.]"  | ("var" ~ (&(spaceCharsEOI) | '.'))) ~ push("var.")
  }

  val rankForma: Rule1[String] = rule {
    ("forma"  | "fma" | "form" | "fo" | "f") ~ (&(spaceCharsEOI) | '.') ~
      push ("form")
  }

  val rankSsp: Rule1[String] = rule {
    ("ssp" | "subsp") ~ (&(spaceCharsEOI) | '.') ~ push("ssp.")
  }

  val subGenus: Rule1[SubGenus] = rule {
    '(' ~ softSpace ~ uninomialWord ~ softSpace ~ ')' ~>
    ((u: UninomialWord) =>
      if (u.str.size < 2 || u.str.last == '.') SubGenus(u, 3)
      else SubGenus(u))
  }

  val uninomialCombo: Rule1[Uninomial] = rule {
    (uninomial ~ softSpace ~ rankUninomial ~ softSpace ~ uninomial) ~>
    ((u1: Uninomial, r: String, u2: Uninomial) =>
      u2.copy(rank = Some(r), parent = Some(u1)))
  }

  val uninomial: Rule1[Uninomial] = rule {
    uninomialWord ~ (space ~ authorship).? ~>
    ((u: UninomialWord, a: Option[Authorship]) =>
        Uninomial(u.str, authorship = a))
  }

  val uninomialWord: Rule1[UninomialWord] = rule {
    abbrGenus | capWord | twoLetterGenera
  }

  val abbrGenus: Rule1[UninomialWord] = rule {
    capture(upperChar ~ lowerChar.? ~ lowerChar.? ~ '.') ~>
    ((w: String) => UninomialWord(w, 3))
  }

  val capWord: Rule1[UninomialWord] = rule {
    capWord2 | capWord1
  }

  val capWord1: Rule1[UninomialWord] = rule {
    capture(upperChar ~ lowerChar ~ oneOrMore(lowerChar) ~ '?'.?) ~>
    ((w: String) => if (w.last == '?') UninomialWord(w, 3)
                    else UninomialWord(w))
  }

  val capWord2: Rule1[UninomialWord] = rule {
    capWord1 ~ '-' ~ word1 ~ &(spaceCharsEOI ++ '(') ~>
    ((w1: UninomialWord, w2: String) => w1.copy(s"${w1.str}-$w2"))
  }

  val twoLetterGenera: Rule1[UninomialWord] = rule {
    capture("Ca" | "Ea" | "Ge" | "Ia" | "Io" | "Io" | "Ix" | "Lo" | "Oa" |
      "Ra" | "Ty" | "Ua" | "Aa" | "Ja" | "Zu" | "La" | "Qu" | "As" | "Ba") ~>
    ((w: String) => UninomialWord(w))
  }

  val word: Rule1[String] = rule {
    (word2 | word1) ~ &(spaceCharsEOI ++ '(')
  }

  val word1: Rule1[String] = rule {
    capture(lowerChar ~ oneOrMore(lowerChar))
  }

  val word2: Rule1[String] = rule {
    word1 ~ '-' ~ word1 ~> ((s1: String, s2: String) => s"$s1-$s2")
  }

  val hybridChar = '×'

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
      ((u: Uninomial, appr: String, ign: String) =>
          Name(uninomial = u, approximation = Some(appr),
               ignored = Some(ign), quality = 3))
  }

  val approxName2: Rule1[Name] = rule {
    (uninomial ~ space ~ word ~ space ~ approximation ~ space ~ anyChars) ~>
      ((u: Uninomial, s: String, appr: String, ign: String) =>
        Name(uninomial = u,
             species = Some(Species(s)),
             approximation = Some(appr),
             ignored = Some(ign),
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
          authorsEx = Some(exau.authors.authors)), quality = 3))
  }

  val combinedAuthorship2: Rule1[Authorship] = rule {
    basionymAuthorship ~ softSpace ~ authorship1 ~>
    ((bau: Authorship, cau: Authorship) =>
        bau.copy(combination = Some(cau.authors), basionymParsed = true, quality = 1))
  }

  val basionymYearMisformed: Rule1[Authorship] = rule {
    '(' ~ softSpace ~ authorsGroup ~ softSpace ~ ')' ~ (softSpace ~ ',').? ~ softSpace ~ year ~>
    ((a: AuthorsGroup, y: Year) => Authorship(authors = a.copy(year = Some(y)), inparenthesis = true,
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
    ((a: AuthorsGroup, y: Year) => a.copy(year = Some(y)))
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
    author2 ~ softSpace ~ filius ~> ((au: Author) => au.copy(filius = true))
  }

  val author2: Rule1[Author] = rule {
    oneOrMore(authorWord).separatedBy(softSpace) ~>
      ((au: Seq[String]) => Author(au.mkString(" ")))
  }

  val unknownAuthor: Rule1[Author] = rule {
    capture("?" |
            (("auct" | "anon" | "ht" | "hort") ~ (&(spaceCharsEOI) | '.'))) ~>
    ((auth: String) => Author(auth, anon = true, quality = 3))
  }

  val authorWord: Rule1[String] = rule {
    authorWord1 | authorWord2 | authorPre
  }

  val authorWord1: Rule1[String] = rule {
    capture("arg." | "et al.{?}" | "et al." | "et al")
  }

  val authorWord2: Rule1[String] = rule {
    capture("d'".? ~ authCharUpper ~ zeroOrMore(authCharUpper | authCharLower) ~ '.'.?) ~>
      ((w: String) => { Util.normAuthWord(w) })
  }

  val authCharLower = CharPredicate(LowerAlpha ++
    "àáâãäåæçèéêëìíîïðñòóóôõöøùúûüýÿāăąćĉčďđ'-ēĕėęěğīĭİıĺľłńņňŏőœŕřśşšţťũūŭůűźżžſǎǔǧșțȳß")

  val authCharUpper = CharPredicate(UpperAlpha ++ "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝĆČĎİĶĹĺĽľŁłŅŌŐŒŘŚŜŞŠŸŹŻŽƒǾȘȚ�")

  val filius = rule {
    "f." | "filius"
  }

  val authorPre: Rule1[String] = rule {
    capture("ab" | "af" | "bis" | "da" | "der" | "des" |
            "den" | "della" | "dela" | "de" | "di" | "du" |
            "la" | "ter" | "van" | "von" | "d'") ~ &(spaceCharsEOI)
  }

  val year: Rule1[Year] = rule {
    yearRange | yearApprox | yearWithParens | yearWithPage |
    yearWithDot | yearWithChar | yearNumber
  }

  val yearRange: Rule1[Year] = rule {
    yearNumber ~ '-' ~ oneOrMore(Digit) ~ zeroOrMore(Alpha ++ "?") ~>
    ((y: Year) => y.copy(quality = 3))
  }

  val yearWithDot: Rule1[Year] = rule {
    yearNumber ~ '.' ~> ((y: Year) => y.copy(quality = 3))
  }

  val yearApprox: Rule1[Year] = rule {
    '[' ~ softSpace ~ yearNumber ~ softSpace ~ ']' ~>
     ((y: Year) => y.copy(quality = 3))
  }

  val yearWithPage: Rule1[Year] = rule {
    (yearWithChar | yearNumber) ~ space ~ ':' ~ space ~ oneOrMore(Digit) ~>
    ((y: Year) => y.copy(quality = 3))
  }

  val yearWithParens: Rule1[Year] = rule {
    '(' ~ softSpace ~ (yearWithChar | yearNumber) ~ softSpace ~ ')' ~>
    ((y: Year) => y.copy(quality = 2))
  }

  val yearWithChar: Rule1[Year] = rule {
    yearNumber ~ Alpha ~> ((y: Year) => y.copy(quality = 2))
  }

  val yearNumber: Rule1[Year] = rule {
    capture(CharPredicate("12") ~ CharPredicate("0789") ~ Digit
      ~ (Digit|'?') ~ '?'.?) ~>
      ((y: String) => if (y.last == '?') Year(y, 3) else Year(y))
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
