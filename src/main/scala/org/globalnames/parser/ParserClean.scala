package org.globalnames.parser

import org.parboiled2.SimpleParser
import scala.collection.immutable.Seq
import org.parboiled2.CharPredicate
import org.parboiled2.CharPredicate.{Digit, Printable, Alpha, LowerAlpha,
                                     UpperAlpha}

class ParserClean extends SimpleParser {
  val sciName: Rule1[SciName] = rule {
    softSpace ~ sciName1 ~ softSpace ~ EOI ~>
    ((n: NamesGroup) => SciName(ast = Some(n)))
  }

  val sciName1: Rule1[NamesGroup] = rule {
   hybridFormula | namedHybrid | sciName2 //approxName | sciName2
  }

  val sciName2: Rule1[NamesGroup] = rule {
    name ~> ((n: Name) => NamesGroup(Seq(n)))
  }

  val hybridFormula: Rule1[NamesGroup] = rule {
    hybridFormula1 | hybridFormula2
  }

  val hybridFormula1: Rule1[NamesGroup] = rule {
    name ~ space ~ hybridChar ~ space ~
    species ~ (space ~ infraspeciesGroup).? ~>
    ((n: Name, s: Species, i: Option[InfraspeciesGroup]) =>
      NamesGroup(
        name = Seq(n, Name(uninomial = n.uninomial, species = Some(s),
                    infraspecies = i)),
        hybrid = true,
        quality = 3))
  }

  val hybridFormula2: Rule1[NamesGroup] = rule {
    name ~ space ~ hybridChar ~ (space ~ name).? ~>
    ((n1: Name, n2: Option[Name]) =>
      n2 match {
        case None => NamesGroup(name = Seq(n1), hybrid = true, quality = 3)
        case Some(n) => NamesGroup(name = Seq(n1,n), hybrid = true)
      }
    )
  }

  val namedHybrid: Rule1[NamesGroup] = rule {
    hybridChar ~ softSpace ~ name ~>
    ((n: Name) => NamesGroup(Seq(n), hybrid = true))
  }

  val name: Rule1[Name] = rule {
    name1 | name2 | name3
  }

  val name1: Rule1[Name] = rule {
    (uninomialCombo | uninomial) ~> ((u: Uninomial) => Name(u))
  }

  val name2: Rule1[Name] = rule {
    uninomialWord ~ space ~ comparison ~ (space ~ species).? ~>
    ((u: UninomialWord, comp: String, s: Option[Species]) =>
      Name(uninomial = Uninomial(u.str, quality = u.quality),
           species = s, comparison = Some(comp), quality = 3))
  }

  val name3: Rule1[Name] = rule {
    uninomialWord ~ space ~ subGenus.? ~ space ~
    species ~ space ~ infraspeciesGroup.? ~>
    ((u: UninomialWord, sg: Option[SubGenus], s: Species,
      ig: Option[InfraspeciesGroup]) =>
      Name(uninomial = Uninomial(str = u.str, quality = u.quality),
           species = Some(s),
           infraspecies = ig))
  }

  val infraspeciesGroup: Rule1[InfraspeciesGroup] = rule {
    oneOrMore(infraspecies).separatedBy(space) ~>
    ((inf: Seq[Infraspecies]) => InfraspeciesGroup(inf))
  }

  val infraspecies: Rule1[Infraspecies] = rule {
    (rank ~ space).? ~ word ~ (space ~ authorship).? ~>
    ((r: Option[String], w: String, a: Option[Authorship]) =>
        Infraspecies(w, r, a))
  }

  val species: Rule1[Species] = rule {
    word ~ (space ~ authorship).? ~>
    ((s: String, a: Option[Authorship]) =>
      Species(s, a))
  }

  val comparison: Rule1[String] = rule {
    "cf" ~ ".".? ~ push("cf.")
  }

  val approximation: Rule1[String] = rule {
    capture("spp." | "spp" | "sp.nr." | "sp. nr." | "nr." | "nr" | "sp.aff." |
      "sp. aff." | "sp." | "sp" | "species" |
      "aff." | "aff" | "monst." | "?")
  }

  val rankUninomial: Rule1[String] = rule {
    capture("sect" | "subsect" | "trib" | "subtrib" | "ser" | "subgen" |
      "fam" | "subfam" | "supertrib") ~ ".".? ~>
    ((r: String) => s"$r.")
  }

  val rank: Rule1[String] = rule {
    rankForma | rankVar | rankSsp | rankOther
  }

  val rankOther: Rule1[String] = rule {
    capture("morph." | "f.sp." | "B" | "mut." | "nat" |
     "nothosubsp." | "convar." | "pseudovar." | "sect." | "ser." |
     "subvar." | "subf." | "race" | "α" | "ββ" | "β" | "γ" | "δ" |
     "ε" | "φ" | "θ" | "μ" | "a." | "b." | "c." | "d." | "e." | "g." |
     "k." | "****" | "**" | "*")
  }

  val rankVar: Rule1[String] = rule {
    ("[var.]"  | ("var" ~ ".".?)) ~ push("var.")
  }

  val rankForma: Rule1[String] = rule {
    ("forma"  | "fma" | "form" | "fo" | "f") ~ ".".? ~ push("f.")
  }

  val rankSsp: Rule1[String] = rule {
    ("ssp" | "subsp") ~ ".".? ~ push("ssp.")
  }

  val subGenus: Rule1[SubGenus] = rule {
    "(" ~ space ~ uninomialWord ~ space ~ ")" ~>
    ((u: UninomialWord) =>
      if (u.str.size < 2 || u.str.last == '.') SubGenus(u, 3)
      else SubGenus(u))
  }

  val uninomialCombo: Rule1[Uninomial] = rule {
    (uninomial ~ space ~ rankUninomial ~ space ~ uninomial) ~>
    ((u1: Uninomial, r: String, u2: Uninomial) =>
      u2.copy(rank = Some(r), parent = Some(u1)))
  }

  val uninomial: Rule1[Uninomial] = rule {
    (uninomialWord ~ space ~ authorship.?) ~>
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
    capWord1 ~ "-" ~ word1 ~>
    ((w1: UninomialWord, w2: String) => w1.copy(s"${w1.str}-$w2"))
  }

  val twoLetterGenera: Rule1[UninomialWord] = rule {
    capture("Ca" | "Ea" | "Ge" | "Ia" | "Io" | "Io" | "Ix" | "Lo" | "Oa" |
      "Ra" | "Ty" | "Ua" | "Aa" | "Ja" | "Zu" | "La" | "Qu" | "As" | "Ba") ~>
    ((w: String) => UninomialWord(w))
  }

  val word: Rule1[String] = rule {
    word2 | word1
  }

  val word1: Rule1[String] = rule {
    capture(lowerChar ~ oneOrMore(lowerChar))
  }

  val word2: Rule1[String] = rule {
    word1 ~ "-" ~ word1 ~> ((s1: String, s2: String) => s"$s1-$s2")
  }

  val hybridChar = CharPredicate("×")

  val upperChar = CharPredicate(UpperAlpha ++ "ËÆŒ")

  val lowerChar = CharPredicate(LowerAlpha ++ "'ëæœſ")

  val anyChars: Rule1[String] = rule { capture(zeroOrMore(ANY)) }

  // val approxName: Rule1[NamesGroup] = rule {
  //   approxName1 | approxName2 ~>
  //   ((n: Name) =>
  //    NamesGroup(name = Seq(n), quality = 3))
  // }

  // val approxName1: Rule1[Name] = rule {
  //   uninomial ~ space ~ approximation ~ space ~ anyChars ~>
  //     ((u: Uninomial, appr: String, ign: String) =>
  //         Name(uninomial = g, approximation = Some(appr),
  //              ignored = Some(ign), quality = 3))
  // }
  //
  // val approxName2: Rule1[Name] = rule {
  //   (uninomial ~ space ~ word ~ space ~ approximation ~ space ~ anyChars) ~>
  //     ((u: Uninomial, s: String, appr: String, ign: String) =>
  //       Name(uninomial = u,
  //            species = Some(Species(s)),
  //            approximation = Some(appr),
  //            ignored = Some(ign),
  //            quality = 3)
  //     )
  // }

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
    basionymAuthorship ~ space ~ authorship1 ~>
    ((bau: Authorship, cau: Authorship) =>
        bau.copy(combination = Some(cau.authors), quality = 1))
  }

  val basionymYearMisformed: Rule1[Authorship] = rule {
    '(' ~ space ~ authorsGroup ~ space ~ ')' ~ (space ~ ',').? ~ space ~ year ~>
    ((a: AuthorsGroup, y: Year) => Authorship(authors = a.copy(year = Some(y)),
                                              quality = 3))
  }

  val basionymAuthorship: Rule1[Authorship] = rule {
    basionymAuthorship1 | basionymAuthorship2
  }

  val basionymAuthorship1: Rule1[Authorship] = rule {
    '(' ~ space ~ authorship1 ~ space ~ ')' ~>
    ((a: Authorship) => a.copy(quality = 2))
  }

  val basionymAuthorship2: Rule1[Authorship] = rule {
    '(' ~ space ~ '(' ~ space ~ authorship1 ~ space ~ ')' ~ space ~ ')' ~>
    ((a: Authorship) => a.copy(quality = 3))
  }

  val authorship1: Rule1[Authorship] = rule {
    (authorsYear | authorsGroup) ~>
    ((a: AuthorsGroup) => Authorship(a))
  }

  val authorsYear: Rule1[AuthorsGroup] = rule {
    authorsGroup ~ space ~ (',' ~ space).? ~ year ~>
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
    space ~ ("," | "&" | "and" | "et") ~ space
  }

  val authorEx = rule {
    space ~ ("ex" | "in") ~ space
  }

  val author: Rule1[Author] = rule {
    author1 | author2 | unknownAuthor
  }

  val author1: Rule1[Author] = rule {
    author2 ~ space ~ filius ~> ((au: Author) => Author(s"$au f."))
  }

  val author2: Rule1[Author] = rule {
    oneOrMore(authorWord).separatedBy(space) ~>
      ((au: Seq[String]) => Author(au.mkString(" ")))
  }

  val unknownAuthor: Rule1[Author] = rule {
    capture("?" |
            (("auct." | "auct" | "anon." | "anon" | "ht." | "ht" | "hort." |
              "hort") ~ &(spaceChars | EOI))) ~>
    ((a: String) => Author(a, true, 3))
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

  val year: Rule1[Year] = rule {
    yearWithParens | yearWithChar | yearNumber
  }

  val yearWithParens: Rule1[Year] = rule {
    '(' ~ space ~ (yearWithChar | yearNumber) ~ space ~ ')' ~>
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

  val spaceChars = CharPredicate("　  \t\r\n\fщ_")

  def calcPos(pos: Option[Vector[Tuple3[Int, Int, String]]], end: Int, name: String) = {
    pos match {
      case None => Some(Vector((0, end, name)))
      case Some(v) => Some(v ++ Vector(((v.last._2 - 1), end, name)))
    }
  }
}
