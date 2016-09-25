package org.globalnames.parser

import java.util.regex.Pattern

import org.parboiled2.CharPredicate.{Alpha, Digit, LowerAlpha, UpperAlpha}
import org.parboiled2._

import scalaz.Scalaz._

import shapeless._

class Parser(val input: ParserInput,
             preprocessChanges: Boolean,
             collectErrors: Boolean)
  extends org.parboiled2.Parser(collectErrors = collectErrors) {

  import Parser._

  type RuleWithWarning[T <: AstNode] = Rule1[NodeMeta[T]]

  def sciName: Rule2[ScientificName, Vector[Warning]] = rule {
    capturePos(softSpace ~ sciName1) ~ unparsed ~ EOI ~> {
      (ng: NodeMeta[NamesGroup], pos: CapturePosition,
       unparsedTail: Option[String]) =>
      val name = input.sliceString(pos.start, pos.end)

      val warnings = Vector(
        doubleSpacePattern.matcher(name).find().option {
          Warning(2, "Multiple adjacent space characters", ng.node)
        },
        name.exists { ch => spaceMiscoded.indexOf(ch) >= 0 }.option {
          Warning(3, "Non-standard space characters", ng.node)
        },
        name.exists { ch => authCharMiscoded == ch }.option {
          Warning(3, "Incorrect conversion to UTF-8", ng.node)
        },
        unparsedTail.map {
          case g if g.trim.isEmpty =>
            Warning(2, "Trailing whitespace", ng.node)
          case _ =>
            Warning(3, "Unparseable tail", ng.node)
        },
        preprocessChanges.option {
          Warning(2, "Name had to be changed by preprocessing", ng.node)
        }
      ).flatten ++ ng.warnings

      val worstLevel = if (warnings.isEmpty) 1
                       else warnings.maxBy { _.level }.level

      ScientificName(namesGroup = ng.node.some, unparsedTail = unparsedTail,
                     quality = worstLevel) :: warnings :: HNil
    }
  }

  def sciName1: RuleWithWarning[NamesGroup] = rule {
    hybridFormula | namedHybrid | approxName | sciName2
  }

  def sciName2: RuleWithWarning[NamesGroup] = rule {
    name ~> { (n: NodeMeta[Name]) =>
      NodeMeta(NamesGroup(Vector(n.node)), n.warnings)
    }
  }

  def hybridFormula: RuleWithWarning[NamesGroup] = rule {
    name ~ space ~ hybridChar ~ (hybridFormula1 | hybridFormula2)
  }

  def hybridFormula1: Rule[NodeMeta[Name] :: HybridChar :: HNil,
                           NodeMeta[NamesGroup] :: HNil] = rule {
    softSpace ~ species ~ (space ~ infraspeciesGroup).? ~> {
      (n: NodeMeta[Name], hc: HybridChar, s: NodeMeta[Species],
       i: Option[NodeMeta[InfraspeciesGroup]]) =>
        val uninomial1 = n.node.uninomial.copy(implied = true)
        val n1 = n.node.copy(genusParsed = true)

        val ng = NamesGroup(
          name = Vector(n1,
                        Name(uninomial = uninomial1,
                             species = s.node.some,
                             infraspecies = i.map { _.node })),
          hybrid = hc.some)

        val warns = n.warnings ++ s.warnings ++ i.map { _.warnings }.orZero
        NodeMeta(ng, Warning(3, "Incomplete hybrid formula", ng) +: warns)
          .changeWarningsRef((n.node, n1), (n.node.uninomial, uninomial1))
    }
  }

  def hybridFormula2: Rule[NodeMeta[Name] :: HybridChar :: HNil,
                           NodeMeta[NamesGroup] :: HNil] = rule {
    (space ~ name).? ~> {
      (n1: NodeMeta[Name], hc: HybridChar, n2: Option[NodeMeta[Name]]) =>
        val ng = n2 match {
          case None => NamesGroup(name = Vector(n1.node), hybrid = hc.some)
          case Some(name2) => NamesGroup(name = Vector(n1.node, name2.node), hybrid = hc.some)
        }
        val warns = Warning(2, "Hybrid formula", ng) +: (n1.warnings ++ n2.map { _.warnings }.orZero)
        NodeMeta(ng, warns)
    }
  }

  def namedHybrid: RuleWithWarning[NamesGroup] = rule {
    hybridChar ~ capturePos(softSpace) ~ name ~> {
      (hc: HybridChar, spacePos: CapturePosition, n: NodeMeta[Name]) =>
        val ng = NamesGroup(Vector(n.node), hybrid = hc.some)
        val warns = Vector(
          (spacePos.start == spacePos.end).option {
            Warning(3, "Hybrid char not separated by space", ng)
          },
          Warning(2, "Named hybrid", ng).some).flatten
        NodeMeta(ng, warns)
    }
  }

  def name: RuleWithWarning[Name] = rule {
    name2 | name3 | name1
  }

  def name1: RuleWithWarning[Name] = rule {
    (uninomialCombo | uninomial) ~> { (u: NodeMeta[Uninomial]) =>
      NodeMeta(Name(u.node), u.warnings)
    }
  }

  def name2: RuleWithWarning[Name] = rule {
    uninomialWord ~ space ~ comparison ~ (space ~ species).? ~>
    {(u: NodeMeta[UninomialWord], c: NodeMeta[Comparison],
      s: Option[NodeMeta[Species]]) =>
      val u1 = Uninomial(u.node.pos)
      val nm = Name(uninomial = u1, species = s.map { _.node }, comparison = c.node.some)
      val warns = u.warnings ++ c.warnings ++ s.map { _.warnings }.orZero
      NodeMeta(nm, Warning(3, "Name comparison", nm) +: warns)
        .changeWarningsRef((u.node, u1))
    }
  }

  def name3: RuleWithWarning[Name] = rule {
    uninomialWord ~ (softSpace ~ subGenus).? ~ softSpace ~
    species ~ (space ~ infraspeciesGroup).? ~> {
      (uw: NodeMeta[UninomialWord],
       maybeSubGenus: Option[NodeMeta[SubGenus]],
       species: NodeMeta[Species],
       maybeInfraspeciesGroup: Option[NodeMeta[InfraspeciesGroup]]) =>
         val u1 = Uninomial(uw.node.pos)
         val node = Name(u1,
                         maybeSubGenus.map { _.node },
                         species = species.node.some,
                         infraspecies = maybeInfraspeciesGroup.map { _.node })
         val warns = uw.warnings ++ maybeSubGenus.map { _.warnings }.orZero ++
                      species.warnings ++ maybeInfraspeciesGroup.map { _.warnings }.orZero
         NodeMeta(node, warns).changeWarningsRef((uw.node, u1))
    }
  }

  def infraspeciesGroup: RuleWithWarning[InfraspeciesGroup] = rule {
    oneOrMore(infraspecies).separatedBy(space) ~>
    { (inf: Seq[NodeMeta[Infraspecies]]) =>
      NodeMeta(InfraspeciesGroup(inf.map { _.node }),
                 inf.flatMap { _.warnings }.toVector)
    }
  }

  def infraspecies: RuleWithWarning[Infraspecies] = rule {
    (rank ~ softSpace).? ~ word ~ (space ~ authorship).? ~>
    { (r: Option[NodeMeta[Rank]], sw: NodeMeta[SpeciesWord],
       a: Option[NodeMeta[Authorship]]) =>
      NodeMeta(Infraspecies(sw.node, r.map { _.node }, a.map { _.node }),
                 r.map { _.warnings }.orZero ++ sw.warnings ++ a.map { _.warnings }.orZero)
    }
  }

  def species: RuleWithWarning[Species] = rule {
    word ~ (softSpace ~ authorship).? ~ &(spaceCharsEOI ++ "(,:.;") ~> {
      (sw: NodeMeta[SpeciesWord], a: Option[NodeMeta[Authorship]]) =>
        NodeMeta(Species(sw.node, a.map { _.node }),
                   sw.warnings ++ a.map { _.warnings }.orZero)
    }
  }

  def comparison: RuleWithWarning[Comparison] = rule {
    capturePos("cf" ~ '.'.?) ~> { (p: CapturePosition) =>
      NodeMeta(Comparison(p), Vector.empty)
    }
  }

  def approximation: RuleWithWarning[Approximation] = rule {
    capturePos("sp.nr." | "sp. nr." | "sp.aff." | "sp. aff." | "monst." | "?" |
      (("spp" | "nr" | "sp" | "aff" | "species") ~ (&(spaceCharsEOI) | '.'))) ~>
      { (p: CapturePosition) => NodeMeta(Approximation(p)) }
  }

  def rankUninomial: RuleWithWarning[Rank] = rule {
    capturePos(("sect" | "subsect" | "trib" | "subtrib" | "subser" | "ser" |
      "subgen" | "fam" | "subfam" | "supertrib") ~ '.'.?) ~ &(spaceCharsEOI) ~>
      { (p: CapturePosition) => NodeMeta(Rank(p)) }
  }

  def rank: RuleWithWarning[Rank] = rule {
    rankForma | rankVar | rankSsp | rankOther
  }

  def rankOther: RuleWithWarning[Rank] = rule {
    capturePos("morph." | "f.sp." | "mut." | "nat" |
     "nothosubsp." | "convar." | "pseudovar." | "sect." | "ser." |
     "subvar." | "subf." | "race" | "α" | "ββ" | "β" | "γ" | "δ" |
     "ε" | "φ" | "θ" | "μ" | "a." | "b." | "c." | "d." | "e." | "g." |
     "k." | "****" | "**" | "*") ~ &(spaceCharsEOI) ~> {
       (p: CapturePosition) =>
         val r = Rank(p)
         val rank = input.sliceString(p.start, p.end)
         val warns = rank match {
           case "*" | "**" | "***" | "****" | "nat" | "f.sp" | "mut." =>
             Vector(Warning(3, "Uncommon rank", r))
           case _ => Vector.empty
         }
         NodeMeta(r, warns)
     }
  }

  def rankVar: RuleWithWarning[Rank] = rule {
    capturePos("[var.]" | ("var" ~ (&(spaceCharsEOI) | '.'))) ~>
      { (p: CapturePosition) => NodeMeta(Rank(p, "var.".some)) }
  }

  def rankForma: RuleWithWarning[Rank] = rule {
    capturePos(("forma"  | "fma" | "form" | "fo" | "f") ~
    (&(spaceCharsEOI) | '.')) ~> { (p: CapturePosition) => NodeMeta(Rank(p, "fm.".some)) }
  }

  def rankSsp: RuleWithWarning[Rank] = rule {
    capturePos(("ssp" | "subsp") ~ (&(spaceCharsEOI) | '.')) ~>
      { (p: CapturePosition) => NodeMeta(Rank(p, "ssp.".some)) }
  }

  def subGenus: RuleWithWarning[SubGenus] = rule {
    '(' ~ softSpace ~ uninomialWord ~ softSpace ~ ')' ~> {
      (u: NodeMeta[UninomialWord]) => NodeMeta(SubGenus(u.node), u.warnings)
    }
  }

  def uninomialCombo: RuleWithWarning[Uninomial] = rule {
    (uninomialCombo1 | uninomialCombo2) ~> { (u: NodeMeta[Uninomial]) =>
      val warns = Warning(2, "Combination of two uninomials", u.node) +: u.warnings
      NodeMeta(u.node, warns)
    }
  }

  def uninomialCombo1: RuleWithWarning[Uninomial] = rule {
    uninomialWord ~ softSpace ~ subGenus ~ softSpace ~ authorship.? ~>
    {(uw: NodeMeta[UninomialWord], sg: NodeMeta[SubGenus], a: Option[NodeMeta[Authorship]]) =>
      val u = Uninomial(sg.node.pos, a.map { _.node },
                        Rank(CapturePosition.empty, typ = "subgen.".some).some,
                        Uninomial(uw.node.pos).some)
      val warns = uw.warnings ++ sg.warnings ++ a.map { _.warnings }.orZero
      NodeMeta(u, warns)
    }
  }

  def uninomialCombo2: RuleWithWarning[Uninomial] = rule {
    (uninomial ~ softSpace ~ rankUninomial ~ softSpace ~ uninomial) ~> {
      (u1: NodeMeta[Uninomial], r: NodeMeta[Rank], u2: NodeMeta[Uninomial]) =>
        val uw = u2.node.copy(rank = r.node.some, parent = u1.node.some)
        val warns = u1.warnings ++ r.warnings ++ u2.warnings
        NodeMeta(uw, warns).changeWarningsRef((u2.node, uw))
      }
  }

  def uninomial: RuleWithWarning[Uninomial] = rule {
    uninomialWord ~ (space ~ authorship).? ~>
    { (u: NodeMeta[UninomialWord], authorship: Option[NodeMeta[Authorship]]) =>
      val u1 = Uninomial(u.node.pos, authorship.map { _.node })
      val warns = u.warnings ++ authorship.map { _.warnings }.orZero
      NodeMeta(u1, warns).changeWarningsRef((u.node, u1))
    }
  }

  def uninomialWord: RuleWithWarning[UninomialWord] = rule {
    abbrGenus | capWord | twoLetterGenera
  }

  def abbrGenus: RuleWithWarning[UninomialWord] = rule {
    capturePos(upperChar ~ lowerChar.? ~ lowerChar.? ~ '.') ~> { (wp: CapturePosition) =>
      val uw = UninomialWord(wp)
      NodeMeta(uw, Vector(Warning(3, "Abbreviated uninomial word", uw)))
    }
  }

  def capWord: RuleWithWarning[UninomialWord] = rule {
    (capWord2 | capWord1) ~> { (uw: NodeMeta[UninomialWord]) => {
      val word = input.sliceString(uw.node.pos.start, uw.node.pos.end)
      val hasForbiddenChars =
        word.exists { ch => sciCharsExtended.indexOf(ch) >= 0 ||
                            sciUpperCharExtended.indexOf(ch) >= 0 }
      val warns = hasForbiddenChars.option {
        Warning(2, "Non-standard characters in canonical", uw.node)
      }.toVector ++ uw.warnings
      NodeMeta(uw.node, warns)
    }}
  }

  def capWord1: RuleWithWarning[UninomialWord] = rule {
    capturePos(upperChar ~ lowerChar ~ oneOrMore(lowerChar) ~ '?'.?) ~> {
      (p: CapturePosition) =>
        val uw = UninomialWord(p)
        val warns = (input.charAt(p.end - 1) == '?').option {
          Warning(3, "Uninomial word with question mark", uw)
        }.toVector
        NodeMeta(uw, warns)
    }
  }

  def capWord2: RuleWithWarning[UninomialWord] = rule {
    capWord1 ~ '-' ~ word1 ~> {
      (uw: NodeMeta[UninomialWord], wPos: CapturePosition) =>
        val uw1 = uw.node.copy(pos = CapturePosition(uw.node.pos.start, wPos.end))
        NodeMeta(uw1, uw.warnings).changeWarningsRef((uw.node, uw1))
    }
  }

  def twoLetterGenera: RuleWithWarning[UninomialWord] = rule {
    capturePos("Ca" | "Ea" | "Ge" | "Ia" | "Io" | "Io" | "Ix" | "Lo" | "Oa" |
      "Ra" | "Ty" | "Ua" | "Aa" | "Ja" | "Zu" | "La" | "Qu" | "As" | "Ba") ~>
    { (p: CapturePosition) => NodeMeta(UninomialWord(p)) }
  }

  def word: RuleWithWarning[SpeciesWord] = rule {
    !(authorPre | rankUninomial | approximation) ~ (word3 | word2 | word1) ~
      &(spaceCharsEOI ++ "(.,:;") ~> {
      (pos: CapturePosition) =>
        val sw = SpeciesWord(pos)
        val word = input.sliceString(pos.start, pos.end)
        val warns = Vector(
          (word.indexOf(apostr) >= 0).option {
            Warning(3, "Apostrophe is not allowed in canonical", sw)
          },
          word(0).isDigit.option { Warning(3, "Numeric prefix", sw) },
          word.exists { ch => sciCharsExtended.indexOf(ch) >= 0 }.option {
            Warning(2, "Non-standard characters in canonical", sw)
          }
        )
        NodeMeta(sw, warns.flatten)
    }
  }

  def word1: Rule1[CapturePosition] = rule {
    capturePos(lowerChar ~ oneOrMore(lowerChar))
  }

  def word2: Rule1[CapturePosition] = rule {
    capturePos(oneOrMore(lowerChar) | (1 to 2).times(CharPredicate(Digit))) ~ dash ~ word1 ~> {
      (p1: CapturePosition, p2: CapturePosition) => CapturePosition(p1.start, p2.end)
    }
  }

  def word3: Rule1[CapturePosition] = rule {
    capturePos(oneOrMore(lowerChar)) ~ apostr ~ word1 ~> {
      (p1: CapturePosition, p2: CapturePosition) => CapturePosition(p1.start, p2.end)
    }
  }

  def hybridChar: Rule1[HybridChar] = rule {
    capturePos('×') ~> { (pos: CapturePosition) => HybridChar(pos) }
  }

  def unparsed: Rule1[Option[String]] = rule {
    capture(wordBorderChar ~ ANY.*).?
  }

  def approxName: RuleWithWarning[NamesGroup] = rule {
    uninomial ~ space ~ (approxName1 | approxName2) ~> {
      (n: NodeMeta[Name]) =>
        val ng = NamesGroup(name = Vector(n.node))
        NodeMeta(ng, Warning(3, "Name is approximate", ng) +: n.warnings)
    }
  }

  def approxNameIgnored: Rule1[Option[String]] = rule {
    (softSpace ~ capture(anyVisible.+ ~ (softSpace ~ anyVisible.+).*)).?
  }

  def approxName1: Rule[NodeMeta[Uninomial] :: HNil,
                        NodeMeta[Name] :: HNil] = rule {
    approximation ~ approxNameIgnored ~>
      { (u: NodeMeta[Uninomial], appr: NodeMeta[Approximation], ign: Option[String]) =>
        val nm = Name(uninomial = u.node, approximation = appr.node.some, ignored = ign)
        NodeMeta(nm, u.warnings ++ appr.warnings)
      }
  }

  def approxName2: Rule[NodeMeta[Uninomial] :: HNil,
                        NodeMeta[Name] :: HNil] = rule {
    word ~ space ~ approximation ~ approxNameIgnored ~>
      { (u: NodeMeta[Uninomial], sw: NodeMeta[SpeciesWord],
         appr: NodeMeta[Approximation], ign: Option[String]) =>
        val nm = Name(uninomial = u.node, species = Species(sw.node).some,
                      approximation = appr.node.some, ignored = ign)
        NodeMeta(nm, u.warnings ++ sw.warnings ++ appr.warnings)
      }
  }

  def authorship: RuleWithWarning[Authorship] = rule {
    (combinedAuthorship | basionymYearMisformed |
     basionymAuthorship | authorship1) ~ &(spaceCharsEOI ++ "(,:")
  }

  def combinedAuthorship: RuleWithWarning[Authorship] = rule {
    combinedAuthorship1 | combinedAuthorship2
  }

  def combinedAuthorship1: RuleWithWarning[Authorship] = rule {
    basionymAuthorship ~ authorEx ~ authorship1 ~>
    { (bau: NodeMeta[Authorship], exau: NodeMeta[Authorship]) =>
      val authors1 = bau.node.authors.copy(authorsEx = exau.node.authors.authors.some)
      val bau1 = bau.node.copy(authors = authors1)
      val warns = bau.warnings ++ exau.warnings
      NodeMeta(bau1, Warning(2, "Ex authors are not required", bau1) +: warns)
        .changeWarningsRef((bau.node.authors, authors1), (bau.node, bau1))
    }
  }

  def combinedAuthorship2: RuleWithWarning[Authorship] = rule {
    basionymAuthorship ~ softSpace ~ authorship1 ~>
    {(bau: NodeMeta[Authorship], cau: NodeMeta[Authorship]) =>
      val bau1 = bau.node.copy(combination = cau.node.authors.some, basionymParsed = true)
      val warns = bau.warnings ++ cau.warnings
      NodeMeta(bau1, warns).changeWarningsRef((bau.node, bau1))
    }
  }

  def basionymYearMisformed: RuleWithWarning[Authorship] = rule {
    '(' ~ softSpace ~ authorsGroup ~ softSpace ~ ')' ~ (softSpace ~ ',').? ~
    softSpace ~ year ~>  {
      (a: NodeMeta[AuthorsGroup], y: NodeMeta[Year]) => {
        val authors1 = a.node.copy(year = y.node.some)
        val as = Authorship(authors = authors1, inparenthesis = true, basionymParsed = true)
        val warns = y.warnings ++ a.warnings
        NodeMeta(as, Warning(2, "Misformed basionym year", as) +: warns)
          .changeWarningsRef((a.node, authors1))
      }
    }
  }

  def basionymAuthorship: RuleWithWarning[Authorship] = rule {
    basionymAuthorship1 | basionymAuthorship2
  }

  def basionymAuthorship1: RuleWithWarning[Authorship] = rule {
    '(' ~ softSpace ~ authorship1 ~ softSpace ~ ')' ~> {
      (a: NodeMeta[Authorship]) =>
        val as = a.node.copy(basionymParsed = true, inparenthesis = true)
        NodeMeta(as, a.warnings).changeWarningsRef((a.node, as))
    }
  }

  def basionymAuthorship2: RuleWithWarning[Authorship] = rule {
    '(' ~ softSpace ~ '(' ~ softSpace ~ authorship1 ~ softSpace ~ ')' ~
    softSpace ~ ')' ~> { (a: NodeMeta[Authorship]) =>
      val as = a.node.copy(basionymParsed = true, inparenthesis = true)
      NodeMeta(as, Warning(3, "Authroship in double parentheses", as) +: a.warnings)
        .changeWarningsRef((a.node, as))
    }
  }

  def authorship1: RuleWithWarning[Authorship] = rule {
    (authorsYear | authorsGroup) ~> { (a: NodeMeta[AuthorsGroup]) =>
      NodeMeta(Authorship(a.node), a.warnings)
    }
  }

  def authorsYear: RuleWithWarning[AuthorsGroup] = rule {
    authorsGroup ~ softSpace ~ (',' ~ softSpace).? ~ year ~>
    { (a: NodeMeta[AuthorsGroup], y: NodeMeta[Year]) =>
      val a1 = a.node.copy(year = y.node.some)
      val warns = a.warnings ++ y.warnings
      NodeMeta(a1, warns).changeWarningsRef((a.node, a1))
    }
  }

  def authorsGroup: RuleWithWarning[AuthorsGroup] = rule {
    authorsTeam ~ (authorEx ~ authorsTeam).? ~>
    { (a: NodeMeta[AuthorsTeam], exAu: Option[NodeMeta[AuthorsTeam]]) =>
      val ag = AuthorsGroup(a.node, exAu.map { _.node })
      val warns =
        exAu.map { _ => Vector(Warning(2, "Ex authors are not required", ag)) }.orZero ++
        a.warnings ++ exAu.map { _.warnings }.orZero
      NodeMeta(ag, warns)
    }
  }

  def authorsTeam: RuleWithWarning[AuthorsTeam] = rule {
    oneOrMore(author).separatedBy(authorSep) ~> {
      (a: Seq[NodeMeta[Author]]) =>
        NodeMeta(AuthorsTeam(a.map {_.node}), a.flatMap{_.warnings}.toVector)
    }
  }

  def authorSep = rule { softSpace ~ ("," | "&" | "and" | "et") ~ softSpace }

  def authorEx = rule { space ~ ("ex" | "in") ~ space }

  def author: RuleWithWarning[Author] = rule {
    (author1 | author2 | unknownAuthor) ~> { (au: NodeMeta[Author]) => {
      val warns =
        (au.node.pos.end - au.node.pos.start < 2).option {
          Warning(3, "Author is too short", au.node)
        }.toVector ++ au.warnings
      NodeMeta(au.node, warns)
    }}
  }

  def author1: RuleWithWarning[Author] = rule {
    author2 ~ softSpace ~ filius ~> {
      (au: NodeMeta[Author], filius: NodeMeta[AuthorWord]) =>
        val au1 = au.node.copy(filius = filius.node.some)
        val warns = au.warnings ++ filius.warnings
        NodeMeta(au1, warns).changeWarningsRef((au.node, au1))
    }
  }

  def author2: RuleWithWarning[Author] = rule {
    authorWord ~ zeroOrMore(authorWordSep) ~ !(':') ~>
    { (au: NodeMeta[AuthorWord], aus: Seq[NodeMeta[AuthorWord]]) => {
        NodeMeta(Author(au.node +: aus.map { _.node }),
                          au.warnings ++ aus.flatMap { _.warnings }.toVector)
      }
    }
  }

  def authorWordSep: RuleWithWarning[AuthorWord] = rule {
    capture(ch(dash) | softSpace) ~ authorWord ~> { (sep: String, aw: NodeMeta[AuthorWord]) => {
      val aw1 = sep match {
        case d if d.length == 1 && d(0) == dash =>
          aw.node.copy(separator = AuthorWordSeparator.Dash)
        case _ => aw.node.copy(separator = AuthorWordSeparator.Space)
      }
      NodeMeta(aw1, aw.warnings).changeWarningsRef((aw.node, aw1))
    }}
  }

  def unknownAuthor: RuleWithWarning[Author] = rule {
    capturePos("?" | (("auct" | "anon" | "ht" | "hort") ~ (&(spaceCharsEOI) | '.'))) ~>
    { (authPos: CapturePosition) =>
      val auth = Author(Seq(AuthorWord(authPos)), anon = true)
      val endsWithQuestion = input.charAt(authPos.end - 1) == '?'
      val warns = Vector(Warning(2, "Author is unknown", auth).some,
                         endsWithQuestion.option(Warning(3, "Author as a question mark", auth)))
      NodeMeta(auth, warns.flatten)
    }
  }

  def authorWord: RuleWithWarning[AuthorWord] = rule {
    (authorWord1 | authorWord2 | authorPre) ~> {
      (aw: NodeMeta[AuthorWord]) => {
        val word = input.sliceString(aw.node.pos.start, aw.node.pos.end)
        val authorIsUpperCase = word.length > 2 &&
          word.forall { ch => ch == '-' || authCharUpperStr.indexOf(ch) >= 0 }
        val warns = authorIsUpperCase.option {
          Warning(2, "Author in upper case", aw.node)
        }.toVector ++ aw.warnings
        NodeMeta(aw.node, warns)
      }
    }
  }

  def authorWord1: RuleWithWarning[AuthorWord] = rule {
    capturePos("arg." | "et al.{?}" | "et al." | "et al") ~> {
      (pos: CapturePosition) => NodeMeta(AuthorWord(pos), Vector.empty)
    }
  }

  def authorWord2: RuleWithWarning[AuthorWord] = rule {
    capturePos("d'".? ~ authCharUpper ~ zeroOrMore(authCharUpper | authCharLower) ~ '.'.?) ~> {
      (pos: CapturePosition) => NodeMeta(AuthorWord(pos), Vector.empty)
    }
  }



  def filius: RuleWithWarning[AuthorWord] = rule {
    capturePos("f." | "fil." | "filius") ~> {
      (pos: CapturePosition) => NodeMeta(AuthorWord(pos), Vector.empty)
    }
  }

  def authorPre: RuleWithWarning[AuthorWord] = rule {
    capturePos("ab" | "af" | "bis" | "da" | "der" | "des" |
               "den" | "della" | "dela" | "de" | "di" | "du" |
               "la" | "ter" | "van" | "von" | "d'") ~ &(spaceCharsEOI) ~> {
      (pos: CapturePosition) => NodeMeta(AuthorWord(pos), Vector.empty)
    }
  }

  def year: RuleWithWarning[Year] = rule {
    yearRange | yearApprox | yearWithParens | yearWithPage |
    yearWithDot | yearWithChar | yearNumber
  }

  def yearRange: RuleWithWarning[Year] = rule {
    yearNumber ~ '-' ~ capturePos(oneOrMore(Digit)) ~ zeroOrMore(Alpha ++ "?") ~>
    { (yStart: NodeMeta[Year], yEnd: CapturePosition) => {
      val yr = yStart.node.copy(approximate = true, rangeEnd = Some(yEnd))
      NodeMeta(yr, Warning(3, "Years range", yr) +: yStart.warnings)
        .changeWarningsRef((yStart.node, yr))
    }}
  }

  def yearWithDot: RuleWithWarning[Year] = rule {
    yearNumber ~ '.' ~> { (y: NodeMeta[Year]) =>
      NodeMeta(y.node, Warning(2, "Year with period", y.node) +: y.warnings)
    }
  }

  def yearApprox: RuleWithWarning[Year] = rule {
    '[' ~ softSpace ~ yearNumber ~ softSpace ~ ']' ~>
      { (y: NodeMeta[Year]) => {
        val yr = y.node.copy(approximate = true)
        NodeMeta(yr, Warning(3, "Year with square brakets", yr) +: y.warnings)
          .changeWarningsRef((y.node, yr))
      }
    }
  }

  def yearWithPage: RuleWithWarning[Year] = rule {
    (yearWithChar | yearNumber) ~ softSpace ~ ':' ~ softSpace ~ oneOrMore(Digit) ~> {
      (y: NodeMeta[Year]) =>
        NodeMeta(y.node, Warning(3, "Year with page info", y.node) +: y.warnings)
    }
  }

  def yearWithParens: RuleWithWarning[Year] = rule {
    '(' ~ softSpace ~ (yearWithChar | yearNumber) ~ softSpace ~ ')' ~> {
      (y: NodeMeta[Year]) => {
        val yr = y.node.copy(approximate = true)
        NodeMeta(yr, Warning(2, "Year with parentheses", yr) +: y.warnings)
          .changeWarningsRef((y.node, yr))
      }}
  }

  def yearWithChar: RuleWithWarning[Year] = rule {
    yearNumber ~ capturePos(Alpha) ~> {
      (y: NodeMeta[Year], pos: CapturePosition) =>
        val yr = y.node.copy(alpha = pos.some)
        NodeMeta(yr, Warning(2, "Year with latin character", yr) +: y.warnings)
          .changeWarningsRef((y.node, yr))
    }
  }

  def yearNumber: RuleWithWarning[Year] = rule {
    capturePos(CharPredicate("12") ~ CharPredicate("0789") ~ Digit ~ (Digit|'?') ~ '?'.?) ~> {
      (yPos: CapturePosition) => {
        val yr = Year(yPos)
        if (input.charAt(yPos.end - 1) == '?') {
          val yr1 = yr.copy(approximate = true)
          NodeMeta(yr1, Vector(Warning(2, "Year with question mark", yr1)))
        } else NodeMeta(yr)
    }}
  }

  def softSpace = rule {
    zeroOrMore(spaceChars)
  }

  def space = rule {
    oneOrMore(spaceChars)
  }
}

object Parser {
  case class NodeMeta[T <: AstNode](node: T,
                                    warnings: Vector[Warning] = Vector.empty) {
    def changeWarningsRef(substitutions: (AstNode, AstNode)*) = {
      val substMap = substitutions.toMap
      val ws = warnings.map { w =>
        substMap.get(w.node).map { subst => w.copy(node = subst) }.getOrElse(w)
      }
      this.copy(warnings = ws)
    }
  }

  final val dash = '-'
  final val spaceMiscoded = "　 \t\r\n\f_"
  final val spaceChars = CharPredicate(" " + spaceMiscoded)
  final val spaceCharsEOI = spaceChars ++ EOI ++ ";"
  final val wordBorderChar = spaceChars ++ CharPredicate(";.,:()]")
  final val sciCharsExtended = "æœſàâåãäáçčéèíìïňññóòôøõöúùüŕřŗššşž"
  final val sciUpperCharExtended = "ÆŒ"
  final val authCharUpperStr =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝĆČĎİĶĹĺĽľŁłŅŌŐŒŘŚŜŞŠŸŹŻŽƒǾȘȚ"
  final val authCharMiscoded = '�'
  final val apostr = '\''
  final val doubleSpacePattern = Pattern.compile("""[\s_]{2}""")
  final val authCharLower = LowerAlpha ++
    "àáâãäåæçèéêëìíîïðñòóóôõöøùúûüýÿāăąćĉčďđ'-ēĕėęěğīĭİıĺľłńņňŏőœŕřśşšţťũūŭůűźżžſǎǔǧșțȳß"
  final val authCharUpper = CharPredicate(authCharUpperStr + authCharMiscoded)
  final val upperChar = UpperAlpha ++ "Ë" ++ sciUpperCharExtended
  final val lowerChar = LowerAlpha ++ "ë" ++ sciCharsExtended
  final val anyVisible = upperChar ++ lowerChar ++ CharPredicate.Visible
}
