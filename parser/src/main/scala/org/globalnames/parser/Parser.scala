package org.globalnames.parser

import ParserWarnings.Warning
import org.parboiled2.CharPredicate.{Alpha, Digit, LowerAlpha, UpperAlpha}
import org.parboiled2.{CapturePos, CharPredicate}

import scalaz.Scalaz._

object Parser extends org.parboiled2.Parser {

  class Context(val parserWarnings: ParserWarnings,
                val preprocessChanges: Boolean)

  private val sciCharsExtended = "æœſàâåãäáçčéèíìïňññóòôøõöúùüŕřŗššşž'"
  private val sciUpperCharExtended = "ÆŒ"
  private val authCharUpperStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
    "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝĆČĎİĶĹĺĽľŁłŅŌŐŒŘŚŜŞŠŸŹŻŽƒǾȘȚ"
  private val authCharMiscoded = '�'
  private val apostr = '\''
  private val spaceMiscoded = "　 \t\r\n\f_"
  private val doubleSpacePattern = """[\s]{2,}""".r

  val sciName: Rule1[ScientificName] = rule {
    capturePos(softSpace ~ sciName1) ~ anyChars ~ EOI ~> {
      (ng: NamesGroup, pos: CapturePos, garbage: String) =>
      val name = state.input.sliceString(pos.start, pos.end)
      if (doubleSpacePattern.findFirstIn(name) != None)
        ctx.parserWarnings
          .add(Warning(2, "Some spaces between words are not single", ng))
      if (name.exists { ch => spaceMiscoded.indexOf(ch) >= 0 })
        ctx.parserWarnings
          .add(Warning(3, "Non-standard space characters", ng))
      if (name.exists { ch => authCharMiscoded == ch })
        ctx.parserWarnings
          .add(Warning(3, "Loss of characters to miscoding", ng))
      garbage match {
        case g if g.isEmpty =>
        case g if g.trim.isEmpty =>
          ctx.parserWarnings.add(Warning(2, "Trailing whitespace", ng))
        case _ =>
          ctx.parserWarnings.add(Warning(3, "Unparseable end", ng))
      }
      if (ctx.preprocessChanges) {
        ctx.parserWarnings
           .add(Warning(3, "Name had to be changed by preprocessing", ng))
      }
      ScientificName(namesGroup = ng.some, garbage = garbage,
                     quality = ctx.parserWarnings.worstLevel)
    }
  }

  val sciName1: Rule1[NamesGroup] = rule {
    hybridFormula | namedHybrid | approxName | sciName2
  }

  val sciName2: Rule1[NamesGroup] = rule {
    name ~> { (n: Name) => NamesGroup(AstNode.id, Vector(n)) }
  }

  val hybridFormula: Rule1[NamesGroup] = rule {
    hybridFormula1 | hybridFormula2
  }

  val hybridFormula1: Rule1[NamesGroup] = rule {
    name ~ space ~ hybridChar ~ softSpace ~
    species ~ (space ~ infraspeciesGroup).? ~>
    { (n: Name, hc: HybridChar, s: Species, i: Option[InfraspeciesGroup]) =>
      val ng = NamesGroup(AstNode.id,
        name = Vector(n.copy(genusParsed = true),
                      Name(AstNode.id,
                           uninomial = n.uninomial.copy(implied = true),
                           species = s.some, infraspecies = i)),
        hybrid = hc.some)
      ctx.parserWarnings.add(Warning(3, "Incomplete hybrid formula", ng))
      ng
    }
  }

  val hybridFormula2: Rule1[NamesGroup] = rule {
    name ~ space ~ hybridChar ~ (space ~ name).? ~> {
      (n1: Name, hc: HybridChar, n2: Option[Name]) =>
        val ng = if (n2 == None) NamesGroup(AstNode.id, name = Vector(n1),
                                             hybrid = hc.some)
                 else NamesGroup(AstNode.id, name = Vector(n1, n2.get),
                                 hybrid = hc.some)
        ctx.parserWarnings.add(Warning(2, "Hbrid formula", ng))
        ng
    }
  }

  val namedHybrid: Rule1[NamesGroup] = rule {
    hybridChar ~ softSpace ~ name ~> { (hc: HybridChar, n: Name) =>
      val ng = NamesGroup(AstNode.id, Vector(n), hybrid = hc.some)
      if (n.uninomial.pos.start == 1) ctx.parserWarnings
        .add(Warning(3, "Hybrid char not separated by space", ng))
      ctx.parserWarnings.add(Warning(2, "Named hybrid", ng))
      ng
    }
  }

  val name: Rule1[Name] = rule {
    name2 | name3 | name1
  }

  val name1: Rule1[Name] = rule {
    (uninomialCombo | uninomial) ~> { (u: Uninomial) => Name(AstNode.id, u) }
  }

  val name2: Rule1[Name] = rule {
    uninomialWord ~ space ~ comparison ~ (space ~ species).? ~>
    {(u: UninomialWord, c: Comparison, s: Option[Species]) =>
      val nm =
        Name(AstNode.id, uninomial = Uninomial(AstNode.id, u.pos),
             species = s, comparison = c.some)
      ctx.parserWarnings.add(Warning(2, "Name comparison", nm))
      nm
    }
  }

  val name3: Rule1[Name] = rule {
    uninomialWord ~ (softSpace ~ subGenus).? ~ softSpace ~
    species ~ (space ~ infraspeciesGroup).? ~>
    {(uw: UninomialWord, maybeSubGenus: Option[SubGenus], species: Species,
      maybeInfraspeciesGroup: Option[InfraspeciesGroup]) =>
      Name(AstNode.id,
           Uninomial(AstNode.id, uw.pos),
           maybeSubGenus,
           species = species.some,
           infraspecies = maybeInfraspeciesGroup)
    }
  }

  val infraspeciesGroup: Rule1[InfraspeciesGroup] = rule {
    oneOrMore(infraspecies).separatedBy(space) ~>
    { (inf: Seq[Infraspecies]) => InfraspeciesGroup(AstNode.id, inf) }
  }

  val infraspecies: Rule1[Infraspecies] = rule {
    (rank ~ softSpace).? ~ word ~ (space ~ authorship).? ~>
    { (r: Option[Rank], sw: SpeciesWord, a: Option[Authorship]) =>
      Infraspecies(AstNode.id, sw.pos, r, a) }
  }

  val species: Rule1[Species] = rule {
    word ~ (softSpace ~ authorship).? ~ &(spaceCharsEOI ++ "(,:") ~> {
      (sw: SpeciesWord, a: Option[Authorship]) => Species(AstNode.id, sw.pos, a)
    }
  }

  val comparison: Rule1[Comparison] = rule {
    capturePos("cf" ~ '.'.?) ~> { (p: CapturePos) => Comparison(AstNode.id, p) }
  }

  val approximation: Rule1[Approximation] = rule {
    capturePos("sp.nr." | "sp. nr." | "sp.aff." | "sp. aff." | "monst." | "?" |
      (("spp" | "nr" | "sp" | "aff" | "species") ~ (&(spaceCharsEOI) | '.'))) ~>
      { (p: CapturePos) => Approximation(AstNode.id, p) }
  }

  val rankUninomial: Rule1[Rank] = rule {
    capturePos(("sect" | "subsect" | "trib" | "subtrib" | "ser" | "subgen" |
      "fam" | "subfam" | "supertrib") ~ '.'.?) ~>
      { (p: CapturePos) => Rank(AstNode.id, p) }
  }

  val rank: Rule1[Rank] = rule {
    rankForma | rankVar | rankSsp | rankOther
  }

  val rankOther: Rule1[Rank] = rule {
    capturePos("morph." | "f.sp." | "mut." | "nat" |
     "nothosubsp." | "convar." | "pseudovar." | "sect." | "ser." |
     "subvar." | "subf." | "race" | "α" | "ββ" | "β" | "γ" | "δ" |
     "ε" | "φ" | "θ" | "μ" | "a." | "b." | "c." | "d." | "e." | "g." |
     "k." | "****" | "**" | "*") ~ &(spaceCharsEOI) ~> {
       (p: CapturePos) =>
         val r = Rank(AstNode.id, p)
         val rank = state.input.sliceString(p.start, p.end)
         rank match {
           case "*" | "**" | "***" | "****" | "nat" =>
             ctx.parserWarnings.add(Warning(3, "Uncommon rank", r))
           case _ =>
         }
        r
     }
  }

  val rankVar: Rule1[Rank] = rule {
    capturePos("[var.]" | ("var" ~ (&(spaceCharsEOI) | '.'))) ~>
      { (p: CapturePos) => Rank(AstNode.id, p, "var.".some) }
  }

  val rankForma: Rule1[Rank] = rule {
    capturePos(("forma"  | "fma" | "form" | "fo" | "f") ~
    (&(spaceCharsEOI) | '.')) ~> { (p: CapturePos) =>
      Rank(AstNode.id, p, "form".some)
    }
  }

  val rankSsp: Rule1[Rank] = rule {
    capturePos(("ssp" | "subsp") ~ (&(spaceCharsEOI) | '.')) ~>
      { (p: CapturePos) => Rank(AstNode.id, p, "ssp.".some) }
  }

  val subGenus: Rule1[SubGenus] = rule {
    '(' ~ softSpace ~ uninomialWord ~ softSpace ~ ')' ~> {
      (u: UninomialWord) =>
        val size = u.pos.end - u.pos.start
        val lastCh = state.input.charAt(u.pos.end - 1)
        val sg = SubGenus(AstNode.id, u)
        if (size < 2 || lastCh == '.') {
          ctx.parserWarnings.add(Warning(3, "Abbreviated subgenus", sg))
        }
        sg
    }
  }

  val uninomialCombo: Rule1[Uninomial] = rule {
    uninomialCombo1 | uninomialCombo2 ~> {
      (u: Uninomial) => ctx.parserWarnings
        .add(Warning(2, "Combination of two uninomials", u))
      u
    }
  }

  val uninomialCombo1: Rule1[Uninomial] = rule {
    uninomialWord ~ softSpace ~ subGenus ~ softSpace ~ authorship.? ~>
    ((uw: UninomialWord, sg: SubGenus, a: Option[Authorship]) =>
      Uninomial(AstNode.id, sg.pos, a,
                Rank(AstNode.id, CapturePos.empty, typ = "subgen.".some).some,
                Uninomial(AstNode.id, uw.pos).some))
  }

  val uninomialCombo2: Rule1[Uninomial] = rule {
    (uninomial ~ softSpace ~ rankUninomial ~ softSpace ~ uninomial) ~>
    ((u1: Uninomial, r: Rank, u2: Uninomial) =>
      u2.copy(rank = r.some, parent = u1.some))
  }

  val uninomial: Rule1[Uninomial] = rule {
    uninomialWord ~ (space ~ authorship).? ~>
    { (u: UninomialWord, authorship: Option[Authorship]) =>
      Uninomial(AstNode.id, u.pos, authorship)
    }
  }

  val uninomialWord: Rule1[UninomialWord] = rule {
    abbrGenus | capWord | twoLetterGenera
  }

  val abbrGenus: Rule1[UninomialWord] = rule {
    capturePos(upperChar ~ lowerChar.? ~ lowerChar.? ~ '.') ~> {
      (wp: CapturePos) => val uw = UninomialWord(AstNode.id, wp)
      ctx.parserWarnings.add(Warning(3, "Genus is abbreviated", uw))
      uw
    }
  }

  val capWord: Rule1[UninomialWord] = rule {
    (capWord2 | capWord1) ~> { (uw: UninomialWord) => {
      val word = state.input.sliceString(uw.pos.start, uw.pos.end)
      val hasForbiddenChars =
        word.exists { ch => sciCharsExtended.indexOf(ch) >= 0 ||
                            sciUpperCharExtended.indexOf(ch) >= 0 }
      if (hasForbiddenChars) {
        ctx.parserWarnings
           .add(Warning(2, "Unimomial has disallowed characters", uw))
      }
      uw
    }}
  }

  val capWord1: Rule1[UninomialWord] = rule {
    capturePos(upperChar ~ lowerChar ~ oneOrMore(lowerChar) ~ '?'.?) ~> {
      (p: CapturePos) =>
        val uw = UninomialWord(AstNode.id, p)
        if (state.input.charAt(p.end - 1) == '?') {
            ctx.parserWarnings
              .add(Warning(3, "Uninomial word ends with question mark", uw))
        }
        uw
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
    { (p: CapturePos) => UninomialWord(AstNode.id, p) }
  }

  val word: Rule1[SpeciesWord] = rule {
    (word2 | word1) ~ &(spaceCharsEOI ++ '(') ~> {
      (pos: CapturePos) =>
        val sw = SpeciesWord(AstNode.id, pos)
        val word = state.input.sliceString(pos.start, pos.end)
        if (word.indexOf(apostr) >= 0)
          ctx.parserWarnings
             .add(Warning(3, "Apostrophe is not allowed in canonical", sw))
        if (word.exists { ch => sciCharsExtended.indexOf(ch) >= 0 }) {
          ctx.parserWarnings
             .add(Warning(2, "Non-standard characters in canonical", sw))
        }
        sw
    }
  }

  val word1: Rule1[CapturePos] = rule {
    capturePos(lowerChar ~ oneOrMore(lowerChar))
  }

  val word2: Rule1[CapturePos] = rule {
    word1 ~ '-' ~ word1 ~> {
      (p1: CapturePos, p2: CapturePos) => CapturePos(p1.start, p2.end)
    }
  }

  val hybridChar: Rule1[HybridChar] = rule {
    capturePos('×') ~> { (pos: CapturePos) => HybridChar(AstNode.id, pos) }
  }

  val upperChar = CharPredicate(UpperAlpha ++ "Ë" ++ sciUpperCharExtended)

  val lowerChar = CharPredicate(LowerAlpha ++ "ë" ++ sciCharsExtended)

  val anyChars: Rule1[String] = rule { capture(zeroOrMore(ANY)) }

  val approxName: Rule1[NamesGroup] = rule {
    (approxName1 | approxName2) ~> { (n: Name) =>
      val ng = NamesGroup(AstNode.id, name = Vector(n))
        ctx.parserWarnings.add(Warning(2, "Name is approximate", ng))
      ng
    }
  }

  val approxName1: Rule1[Name] = rule {
    uninomial ~ space ~ approximation ~ softSpace ~ anyChars ~>
      { (u: Uninomial, appr: Approximation, ign: String) =>
        Name(AstNode.id, uninomial = u, approximation = appr.some,
                      ignored = ign.some)
      }
  }

  val approxName2: Rule1[Name] = rule {
    (uninomial ~ space ~ word ~ space ~ approximation ~ space ~ anyChars) ~>
      { (u: Uninomial, sw: SpeciesWord, appr: Approximation, ign: String) =>
        Name(AstNode.id, uninomial = u,
                      species = Species(AstNode.id, sw.pos).some,
                      approximation = appr.some,
                      ignored = ign.some)
      }
  }

  val authorship: Rule1[Authorship] = rule {
    (combinedAuthorship | basionymYearMisformed |
     basionymAuthorship | authorship1) ~ &(spaceCharsEOI ++ "(,:")
  }

  val combinedAuthorship: Rule1[Authorship] = rule {
    combinedAuthorship1 | combinedAuthorship2
  }

  val combinedAuthorship1: Rule1[Authorship] = rule {
    basionymAuthorship ~ authorEx ~ authorship1 ~>
    {(bau: Authorship, exau: Authorship) =>
      val bau1 =
        bau.copy(authors = bau.authors.copy(authorsEx = exau.authors.authors.some))
      ctx.parserWarnings
        .add(Warning(2, "Ex authors are not required by codes", bau1))
      bau1
    }
  }

  val combinedAuthorship2: Rule1[Authorship] = rule {
    basionymAuthorship ~ softSpace ~ authorship1 ~>
    {(bau: Authorship, cau: Authorship) =>
      val bau1 = bau.copy(combination = cau.authors.some, basionymParsed = true)
      bau1
    }
  }

  val basionymYearMisformed: Rule1[Authorship] = rule {
    '(' ~ softSpace ~ authorsGroup ~ softSpace ~ ')' ~ (softSpace ~ ',').? ~
    softSpace ~ year ~>  { (a: AuthorsGroup, y: Year) =>
      val as = Authorship(AstNode.id, authors = a.copy(year = y.some),
                          inparenthesis = true, basionymParsed = true)
      ctx.parserWarnings.add(Warning(1, "Basionym year is misformed", as))
      as
    }
  }

  val basionymAuthorship: Rule1[Authorship] = rule {
    basionymAuthorship1 | basionymAuthorship2
  }

  val basionymAuthorship1: Rule1[Authorship] = rule {
    '(' ~ softSpace ~ authorship1 ~ softSpace ~ ')' ~> { (a: Authorship) =>
      val as = a.copy(basionymParsed = true, inparenthesis = true)
      as
    }
  }

  val basionymAuthorship2: Rule1[Authorship] = rule {
    '(' ~ softSpace ~ '(' ~ softSpace ~ authorship1 ~ softSpace ~ ')' ~
    softSpace ~ ')' ~> { (a: Authorship) =>
      val as = a.copy(basionymParsed = true, inparenthesis = true)
      ctx.parserWarnings.add(Warning(3, "Double parentheses in authroship", as))
      as
    }
  }

  val authorship1: Rule1[Authorship] = rule {
    (authorsYear | authorsGroup) ~> {(a: AuthorsGroup) => Authorship(AstNode.id, a)}
  }

  val authorsYear: Rule1[AuthorsGroup] = rule {
    authorsGroup ~ softSpace ~ (',' ~ softSpace).? ~ year ~>
    ((a: AuthorsGroup, y: Year) => a.copy(year = y.some))
  }

  val authorsGroup: Rule1[AuthorsGroup] = rule {
    authorsTeam ~ (authorEx ~ authorsTeam).? ~>
    { (a: AuthorsTeam, exAu: Option[AuthorsTeam]) =>
      val ag = AuthorsGroup(AstNode.id, a, exAu)
      if (exAu != None) {
        ctx.parserWarnings
          .add(Warning(2, "Ex authors are not required by codes", ag))
      }
      ag
    }
  }

  val authorsTeam: Rule1[AuthorsTeam] = rule {
    oneOrMore(author).separatedBy(authorSep) ~>
    { (a: Seq[Author]) => AuthorsTeam(AstNode.id, a) }
  }

  val authorSep = rule {
    softSpace ~ ("," | "&" | "and" | "et") ~ softSpace
  }

  val authorEx = rule {
    space ~ ("ex" | "in") ~ space
  }

  val author: Rule1[Author] = rule {
    (author1 | author2 | unknownAuthor) ~> {
      (au: Author) =>
        if (au.pos.end - au.pos.start < 2) ctx.parserWarnings
          .add(Warning(3, "Author name is too short", au))
      au
    }
  }

  val author1: Rule1[Author] = rule {
    author2 ~ softSpace ~ filius ~> { (au: Author, filius: AuthorWord) =>
      au.copy(filius = filius.some)
    }
  }

  val author2: Rule1[Author] = rule {
    oneOrMore(authorWord).separatedBy(softSpace) ~ !(':') ~>
    { (au: Seq[AuthorWord]) => Author(AstNode.id, au) }
  }

  val unknownAuthor: Rule1[Author] = rule {
    capturePos("?" |
            (("auct" | "anon" | "ht" | "hort") ~ (&(spaceCharsEOI) | '.'))) ~>
    { (authPos: CapturePos) =>
      val auth = Author(AstNode.id, Seq(AuthorWord(AstNode.id, authPos)), anon = true)
      ctx.parserWarnings.add(Warning(2, "Author is unknown", auth))
      if (state.input.charAt(authPos.end - 1) == '?') ctx.parserWarnings
        .add(Warning(3, "Unknown author represented by question mark", auth))
      auth
    }
  }

  val authorWord: Rule1[AuthorWord] = rule {
    (authorWord1 | authorWord2 | authorPre) ~> {
      (aw: AuthorWord) => {
        val word = state.input.sliceString(aw.pos.start, aw.pos.end)
        val wordSet = word.toSet - '-'
        if (word.size > 2 &&
            word.forall { ch => ch == '-' || authCharUpperStr.indexOf(ch) >= 0 }) {
            ctx.parserWarnings
              .add(Warning(2, "Authorship word is in upper case", aw))
        }
        aw
      }
    }
  }

  val authorWord1: Rule1[AuthorWord] = rule {
    capturePos("arg." | "et al.{?}" | "et al." | "et al") ~> {
      (pos: CapturePos) => AuthorWord(AstNode.id, pos)
    }
  }

  val authorWord2: Rule1[AuthorWord] = rule {
    capturePos("d'".? ~ authCharUpper ~
    zeroOrMore(authCharUpper | authCharLower) ~ '.'.?) ~> { (pos: CapturePos) =>
      AuthorWord(AstNode.id, pos)
    }
  }

  val authCharLower = CharPredicate(LowerAlpha ++
    "àáâãäåæçèéêëìíîïðñòóóôõöøùúûüýÿāăąćĉčďđ'-ēĕėęěğīĭİıĺľłńņňŏőœŕřśşšţťũūŭůűźżžſǎǔǧșțȳß")

  val authCharUpper = CharPredicate(authCharUpperStr + authCharMiscoded)

  val filius: Rule1[AuthorWord] = rule {
    capturePos("f." | "filius") ~> { (pos: CapturePos) =>
      AuthorWord(AstNode.id, pos)
    }
  }

  val authorPre: Rule1[AuthorWord] = rule {
    capturePos("ab" | "af" | "bis" | "da" | "der" | "des" |
            "den" | "della" | "dela" | "de" | "di" | "du" |
            "la" | "ter" | "van" | "von" | "d'") ~ &(spaceCharsEOI) ~>
      { (pos: CapturePos) => AuthorWord(AstNode.id, pos) }
  }

  val year: Rule1[Year] = rule {
    yearRange | yearApprox | yearWithParens | yearWithPage |
    yearWithDot | yearWithChar | yearNumber
  }

  val yearRange: Rule1[Year] = rule {
    yearNumber ~ '-' ~ oneOrMore(Digit) ~ zeroOrMore(Alpha ++ "?") ~>
    { (y: Year) => {
      val yr = y.copy(approximate = true)
      ctx.parserWarnings.add(Warning(3, "Year is ranged", y))
      yr
    }}
  }

  val yearWithDot: Rule1[Year] = rule {
    yearNumber ~ '.' ~> { (y: Year) => {
      ctx.parserWarnings.add(Warning(2, "Year is with dot", y))
      y
    }}
  }

  val yearApprox: Rule1[Year] = rule {
    '[' ~ softSpace ~ yearNumber ~ softSpace ~ ']' ~>
      { (y: Year) => {
        val yr = y.copy(approximate = true)
        ctx.parserWarnings.add(Warning(3, "Year in square brakets", y))
        yr
      }
    }
  }

  val yearWithPage: Rule1[Year] = rule {
    (yearWithChar | yearNumber) ~ space ~ ':' ~ space ~ oneOrMore(Digit) ~>
    { (y: Year) => {
      ctx.parserWarnings.add(Warning(3, "Year is with page", y))
      y
    }}
  }

  val yearWithParens: Rule1[Year] = rule {
    '(' ~ softSpace ~ (yearWithChar | yearNumber) ~ softSpace ~ ')' ~>
    { (y: Year) => {
      val yr = y.copy(approximate = true)
      ctx.parserWarnings.add(Warning(2, "Year is with parentheses", yr))
      yr
    }}
  }

  val yearWithChar: Rule1[Year] = rule {
    yearNumber ~ capturePos(Alpha) ~> { (y: Year, pos: CapturePos) =>
      val yr = y.copy(alpha = pos.some)
      ctx.parserWarnings.add(Warning(2, "Year has alpha", yr))
      yr
    }
  }

  val yearNumber: Rule1[Year] = rule {
    capturePos(CharPredicate("12") ~ CharPredicate("0789") ~ Digit ~
      (Digit|'?') ~ '?'.?) ~> { (yPos: CapturePos) => {
        val yr = Year(AstNode.id, yPos)
        if (state.input.charAt(yPos.end - 1) == '?') {
          ctx.parserWarnings.add(Warning(2, "Year ends with question mark", yr))
          yr.copy(approximate = true)
        } else yr
    }}
  }

  val softSpace = rule {
    zeroOrMore(spaceChars)
  }

  val space = rule {
    oneOrMore(spaceChars)
  }

  val spaceChars = CharPredicate(" " + spaceMiscoded)

  val spaceCharsEOI = spaceChars ++ EOI ++ ";"
}
