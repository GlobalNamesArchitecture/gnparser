package org.globalnames.parser

import java.util.regex.Pattern

import org.parboiled2.CharPredicate.{Alpha, Digit, LowerAlpha, UpperAlpha}
import org.parboiled2._

import scalaz.{Name => _, _}
import Scalaz._

import shapeless._

class Parser(preprocessorResult: Preprocessor.Result,
             collectErrors: Boolean)
  extends org.parboiled2.Parser() {

  import Parser._

  type RuleNodeMeta[T <: AstNode] = Rule1[NodeMeta[T]]

  override implicit val input: ParserInput = preprocessorResult.unescaped

  override def errorTraceCollectionLimit: Int = 0

  def sciName: Rule2[ScientificName, Vector[Warning]] = rule {
    capturePos(softSpace ~ sciName1) ~ unparsed ~ EOI ~> {
      (ng: NodeMeta[NamesGroup], pos: CapturePosition, unparsedTail: Option[String]) =>
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
          case g if g.trim.isEmpty => Warning(2, "Trailing whitespace", ng.node)
          case _                   => Warning(3, "Unparseable tail", ng.node)
        }
      ).flatten ++ ng.warnings ++ preprocessorResult.warnings.map { wi => Warning(wi, ng.node) }

      val worstLevel = warnings.isEmpty ? 1 | warnings.maxBy { _.level }.level
      val surrogatePreprocessed = preprocessorResult.surrogate.getOrElse(false)
      val sn = ScientificName(namesGroup = ng.node.some, unparsedTail = unparsedTail,
                              quality = worstLevel,
                              surrogatePreprocessed = surrogatePreprocessed)
      sn :: warnings :: HNil
    }
  }

  def sciName1: RuleNodeMeta[NamesGroup] = rule {
    hybridFormula | namedHybrid | approxName | sciName2
  }

  def sciName2: RuleNodeMeta[NamesGroup] = rule {
    name ~> { (n: NodeMeta[Name]) => FactoryAST.namesGroup(n) }
  }

  private type HybridFormula1Type =
    (HybridChar, NodeMeta[Species], Option[NodeMeta[InfraspeciesGroup]])
  private type HybridFormula2Type = (HybridChar, Option[NodeMeta[Name]])

  def hybridFormula: RuleNodeMeta[NamesGroup] = rule {
    name ~ oneOrMore(space ~ (hybridFormula1 | hybridFormula2)) ~> {
      (n1M: NodeMeta[Name], hybsM: Seq[Either[HybridFormula1Type, HybridFormula2Type]]) =>
        val isFormula1 = hybsM.exists { _.isLeft }
        val isFormula2emptyName = hybsM.exists { h => h.isRight && h.right.get._2.isEmpty }
        val isFormula2 = isFormula2emptyName || hybsM.exists { _.isRight }

        val n2M = isFormula1 ? n1M.map { n => n.copy(genusParsed = true) } | n1M
        val hybs1M = hybsM.map {
          case Left((hc, sp, ig)) =>
            val uninomial1M = nodeToMeta(n1M.node.uninomial.copy(implied = true))
            val r = FactoryAST.name(uninomial = uninomial1M,
                                    species = sp.some,
                                    infraspecies = ig)
                    .changeWarningsRef((n1M.node, n2M.node), (n1M.node.uninomial, uninomial1M.node))
            (hc, r.some)
          case Right((hc, n)) => (hc, n)
        }
        val r0 = FactoryAST.namesGroup(n2M, hybs1M)
        val r1 = isFormula2 ? r0.add(warnings = Seq((2, "Hybrid formula"))) | r0
        val r2 = isFormula1 ? r1.add(warnings = Seq((3, "Incomplete hybrid formula"))) | r1
        isFormula2emptyName ? r2.add(warnings = Seq((2, "Probably incomplete hybrid formula"))) | r2
    }
  }

  def hybridFormula1: Rule1[Either[HybridFormula1Type, HybridFormula2Type]] = rule {
    hybridChar ~ softSpace ~ species ~ (space ~ infraspeciesGroup).? ~> {
      (hc: HybridChar, sp: NodeMeta[Species], ig: Option[NodeMeta[InfraspeciesGroup]]) =>
        Left((hc, sp, ig))
    }
  }

  def hybridFormula2: Rule1[Either[HybridFormula1Type, HybridFormula2Type]] = rule {
    hybridChar ~ (space ~ name).? ~> {
      (hc: HybridChar, n: Option[NodeMeta[Name]]) => Right((hc, n))
    }
  }

  def namedHybrid: RuleNodeMeta[NamesGroup] = rule {
    hybridChar ~ capturePos(softSpace) ~ name ~> {
      (hc: HybridChar, spacePos: CapturePosition, n: NodeMeta[Name]) =>
        val ng = FactoryAST.namesGroup(n, namedHybrid = hc.some)
        val warns = Vector(
          (spacePos.start == spacePos.end).option { (3, "Hybrid char not separated by space") },
          (2, "Named hybrid").some).flatten
        ng.add(warnings = warns)
    }
  }

  def name: RuleNodeMeta[Name] = rule {
    name2 | name3 | name4 | name1
  }

  def name1: RuleNodeMeta[Name] = rule {
    (uninomialCombo | uninomial) ~> { (u: NodeMeta[Uninomial]) => FactoryAST.name(u) }
  }

  def name4: RuleNodeMeta[Name] = rule {
    uninomialWord ~ space ~ approximation ~ (space ~ species).? ~> {
      (uM: NodeMeta[UninomialWord], apprM: NodeMeta[Approximation],
       spM: Option[NodeMeta[Species]]) =>
        FactoryAST.name(FactoryAST.uninomial(uM), approximation = apprM.some)
                  .add(warnings = Seq((3, "Name is approximate")))
    }
  }

  def name2: RuleNodeMeta[Name] = rule {
    uninomialWord ~ space ~ comparison ~ (space ~ species).? ~> {
      (u: NodeMeta[UninomialWord], c: NodeMeta[Comparison], s: Option[NodeMeta[Species]]) =>
        val u1 = FactoryAST.uninomial(u)
        val nm = FactoryAST.name(uninomial = u1, species = s, comparison = c.some)
        nm.add(warnings = Seq((3, "Name comparison")))
          .changeWarningsRef((u.node, u1.node))
    }
  }

  def name3: RuleNodeMeta[Name] = rule {
    uninomialWord ~ (softSpace ~
      (subGenus ~> { Left(_: NodeMeta[SubGenus]) } |
       (subGenusOrSuperspecies ~> { Right(_: NodeMeta[SpeciesWord]) }))).? ~
    space ~ species ~ (space ~ comparison).? ~ (space ~ infraspeciesGroup).? ~> {
      (uwM: NodeMeta[UninomialWord],
       eitherGenusSuperspeciesM: Option[Either[NodeMeta[SubGenus], NodeMeta[SpeciesWord]]],
       speciesM: NodeMeta[Species],
       maybeComparisonM: Option[NodeMeta[Comparison]],
       maybeInfraspeciesGroupM: Option[NodeMeta[InfraspeciesGroup]]) =>
         val uM1 = FactoryAST.uninomial(uwM)
         val name = eitherGenusSuperspeciesM match {
           case None => FactoryAST.name(uM1, species = speciesM.some,
                                        comparison = maybeComparisonM,
                                        infraspecies = maybeInfraspeciesGroupM)
           case Some(Left(sgM)) =>
             FactoryAST.name(uM1, sgM.some, species = speciesM.some,
                             comparison = maybeComparisonM,
                             infraspecies = maybeInfraspeciesGroupM)
           case Some(Right(ssM)) =>
             val nm = for { _ <- ssM; u1 <- uM1; species <- speciesM;
                            cmp <- lift(maybeComparisonM);
                              infrOpt <- lift(maybeInfraspeciesGroupM) }
                        yield Name(u1, comparison = cmp,
                                   species = species.some, infraspecies = infrOpt)
             nm.changeWarningsRef((ssM.node, uM1.node))
         }
         name.changeWarningsRef((uwM.node, uM1.node))
    }
  }

  def infraspeciesGroup: RuleNodeMeta[InfraspeciesGroup] = rule {
    oneOrMore(infraspecies).separatedBy(space) ~> {
      (infs: Seq[NodeMeta[Infraspecies]]) => FactoryAST.infraspeciesGroup(infs)
    }
  }

  def infraspecies: RuleNodeMeta[Infraspecies] = rule {
    (rank ~ softSpace).? ~ !(authorEx) ~ word ~ (space ~ authorship).? ~> {
      (r: Option[NodeMeta[Rank]], sw: NodeMeta[SpeciesWord], a: Option[NodeMeta[Authorship]]) =>
        FactoryAST.infraspecies(sw, r, a)
    }
  }

  def species: RuleNodeMeta[Species] = rule {
     !(authorEx) ~ word ~ (softSpace ~ authorship).? ~ &(spaceCharsEOI ++ "(") ~> {
      (sw: NodeMeta[SpeciesWord], a: Option[NodeMeta[Authorship]]) => FactoryAST.species(sw, a)
    }
  }

  def comparison: RuleNodeMeta[Comparison] = rule {
    capturePos("cf" ~ '.'.?) ~> { (p: CapturePosition) => FactoryAST.comparison(p) }
  }

  def approximation: RuleNodeMeta[Approximation] = rule {
    capturePos("sp." ~ spaceChars.? ~ "nr." | "sp." ~ spaceChars.? ~ "aff." | "monst." | "?" |
               (("spp" | "nr" | "sp" | "aff" | "species") ~ (&(spaceCharsEOI) | '.'))) ~> {
      (p: CapturePosition) => FactoryAST.approximation(p) }
  }

  def rankUninomial: RuleNodeMeta[Rank] = rule {
    capturePos(("sect" | "subsect" | "trib" | "subtrib" | "subser" | "ser" |
                "subgen" | "fam" | "subfam" | "supertrib") ~ '.'.?) ~ &(spaceCharsEOI) ~> {
      (p: CapturePosition) => FactoryAST.rank(p)
    }
  }

  def rank: RuleNodeMeta[Rank] = rule {
    rankForma | rankVar | rankSsp | rankOther | rankOtherUncommon
  }

  def rankOtherUncommon: RuleNodeMeta[Rank] = rule {
    capturePos("*" | "nat" | "f.sp" | "mut.") ~ &(spaceCharsEOI) ~> {
      (p: CapturePosition) => FactoryAST.rank(p).add(warnings = Seq((3, "Uncommon rank")))
    }
  }

  def rankOther: RuleNodeMeta[Rank] = rule {
    capturePos("morph." | "nothosubsp." | "convar." | "pseudovar." | "sect." | "ser." | "subvar." |
               "subf." | "race" | "α" | "ββ" | "β" | "γ" | "δ" | "ε" | "φ" | "θ" | "μ" | "a." |
               "b." | "c." | "d." | "e." | "g." | "k." | "pv." | "pathovar." |
               ("ab." ~ (softSpace ~ "n.").?) | "st.") ~
      &(spaceCharsEOI) ~> { (p: CapturePosition) => FactoryAST.rank(p) }
  }

  def rankVar: RuleNodeMeta[Rank] = rule {
    capturePos("variety" | "[var.]" | "nvar." | ("var" ~ (&(spaceCharsEOI) | '.'))) ~> {
      (pos: CapturePosition) =>
        val varStr = (pos.end - pos.start == "nvar.".length) ? "nvar." | "var."
        FactoryAST.rank(pos, varStr.some)
    }
  }

  def rankForma: RuleNodeMeta[Rank] = rule {
    capturePos(("forma" | "fma" | "form" | "fo" | "f") ~ (&(spaceCharsEOI) | '.')) ~> {
      (p: CapturePosition) => FactoryAST.rank(p, "fm.".some)
    }
  }

  def rankSsp: RuleNodeMeta[Rank] = rule {
    capturePos(("ssp" | "subsp") ~ (&(spaceCharsEOI) | '.')) ~> {
      (p: CapturePosition) => FactoryAST.rank(p, "ssp.".some)
    }
  }

  def subGenusOrSuperspecies: RuleNodeMeta[SpeciesWord] = rule {
    ('(' ~ softSpace ~ word ~ softSpace ~ ')') ~> { (wM: NodeMeta[SpeciesWord]) =>
      wM.add(Seq((2, "Ambiguity: subgenus or superspecies found")))
    }
  }

  def subGenus: RuleNodeMeta[SubGenus] = rule {
    '(' ~ softSpace ~ uninomialWord ~ softSpace ~ ')' ~> {
      (u: NodeMeta[UninomialWord]) => FactoryAST.subGenus(u)
    }
  }

  def uninomialCombo: RuleNodeMeta[Uninomial] = rule {
    (uninomialCombo1 | uninomialCombo2) ~> { (u: NodeMeta[Uninomial]) =>
      u.add(warnings = Seq((2, "Combination of two uninomials")))
    }
  }

  def uninomialCombo1: RuleNodeMeta[Uninomial] = rule {
    uninomialWord ~ softSpace ~ subGenus ~ softSpace ~ authorship.? ~> {
      (uw: NodeMeta[UninomialWord], sg: NodeMeta[SubGenus], a: Option[NodeMeta[Authorship]]) =>
        FactoryAST.uninomial(sg.map { _.word }, a,
                             FactoryAST.rank(CapturePosition.empty, typ = "subgen.".some).some,
                             FactoryAST.uninomial(uw).some)
    }
  }

  def uninomialCombo2: RuleNodeMeta[Uninomial] = rule {
    (uninomial ~ softSpace ~ rankUninomial ~ softSpace ~ uninomial) ~> {
      (u1M: NodeMeta[Uninomial], rM: NodeMeta[Rank], u2M: NodeMeta[Uninomial]) =>
        val r = for { u1 <- u1M; r <- rM; u2 <- u2M } yield u2.copy(rank = r.some, parent = u1.some)
        r.changeWarningsRef((u2M.node, r.node))
      }
  }

  def uninomial: RuleNodeMeta[Uninomial] = rule {
    uninomialWord ~ (space ~ authorship).? ~> {
      (uM: NodeMeta[UninomialWord], aM: Option[NodeMeta[Authorship]]) =>
        val r = for { u <- uM; a <- lift(aM) } yield Uninomial(u, a)
        r.changeWarningsRef((uM.node, r.node))
    }
  }

  def uninomialWord: RuleNodeMeta[UninomialWord] = rule {
    abbrGenus | capWord | twoLetterGenera
  }

  def abbrGenus: RuleNodeMeta[UninomialWord] = rule {
    capturePos(upperChar ~ lowerChar.? ~ '.') ~> { (wp: CapturePosition) =>
      FactoryAST.uninomialWord(wp).add(warnings = Seq((3, "Abbreviated uninomial word")))
    }
  }

  def capWord: RuleNodeMeta[UninomialWord] = rule {
    (capWord2 | capWord1) ~> { (uw: NodeMeta[UninomialWord]) => {
      val word = input.sliceString(uw.node.pos.start, uw.node.pos.end)
      val hasForbiddenChars = word.exists { ch => sciCharsExtended.indexOf(ch) >= 0 ||
                                                  sciUpperCharExtended.indexOf(ch) >= 0 }
      uw.add(warnings =
        hasForbiddenChars.option { (2, "Non-standard characters in canonical") }.toVector)
    }}
  }

  def capWord1: RuleNodeMeta[UninomialWord] = rule {
    capturePos(upperChar ~ lowerChar ~ oneOrMore(lowerChar) ~ '?'.?) ~> {
      (p: CapturePosition) =>
        val warns = (input.charAt(p.end - 1) == '?').option {
          (3, "Uninomial word with question mark")
        }.toVector
        FactoryAST.uninomialWord(p).add(warnings = warns)
    }
  }

  def capWord2: RuleNodeMeta[UninomialWord] = rule {
    capWord1 ~ dash ~ (capWord1 |
                      word1 ~> { (w: CapturePosition) => FactoryAST.uninomialWord(w) }) ~> {
      (uwM: NodeMeta[UninomialWord], wM: NodeMeta[UninomialWord]) =>
        val uw1M = for { uw <- uwM; w <- wM }
                   yield uw.copy(pos = CapturePosition(uw.pos.start, w.pos.end))
        uw1M.changeWarningsRef((uwM.node, uw1M.node), (wM.node, uw1M.node))
    }
  }

  def twoLetterGenera: RuleNodeMeta[UninomialWord] = rule {
    capturePos("Ca" | "Ea" | "Ge" | "Ia" | "Io" | "Ix" | "Lo" | "Oa" |
      "Ra" | "Ty" | "Ua" | "Aa" | "Ja" | "Zu" | "La" | "Qu" | "As" | "Ba") ~>
    { (p: CapturePosition) => FactoryAST.uninomialWord(p) }
  }

  def word: RuleNodeMeta[SpeciesWord] = rule {
    !(authorPrefix | rankUninomial | approximation | word4) ~
      (word3 | word2StartDigit | word2 | word1) ~ &(spaceCharsEOI ++ "()") ~> {
      (pos: CapturePosition) =>
        val word = input.sliceString(pos.start, pos.end)
        val warns = Vector(
          (word.indexOf(apostr) >= 0).option { (3, "Apostrophe is not allowed in canonical") },
          word(0).isDigit.option { (3, "Numeric prefix") },
          word.exists { ch => sciCharsExtended.indexOf(ch) >= 0 }.option {
            (2, "Non-standard characters in canonical")
          }
        )
        FactoryAST.speciesWord(pos).add(warnings = warns.flatten)
    }
  }

  def word4: Rule0 = rule {
    oneOrMore(lowerChar) ~ '.' ~ lowerChar
  }

  def word1: Rule1[CapturePosition] = rule {
    capturePos((LowerAlpha ~ dash).? ~ lowerChar ~ oneOrMore(lowerChar))
  }

  def word2StartDigit: Rule1[CapturePosition] = rule {
    capturePos(digitNonZero) ~ Digit.? ~ word2sep.? ~
      3.times(lowerChar) ~ capturePos(oneOrMore(lowerChar)) ~> {
      (p1: CapturePosition, p2: CapturePosition) => CapturePosition(p1.start, p2.end)
    }
  }

  def word2: Rule1[CapturePosition] = rule {
    capturePos(oneOrMore(lowerChar) ~ dash.? ~ oneOrMore(lowerChar))
  }

  def word3: Rule1[CapturePosition] = rule {
    capturePos(lowerChar) ~ zeroOrMore(lowerChar) ~ apostr ~ word1 ~> {
      (p1: CapturePosition, p2: CapturePosition) => CapturePosition(p1.start, p2.end)
    }
  }

  def hybridChar: Rule1[HybridChar] = rule {
    capturePos('×') ~> { (pos: CapturePosition) => HybridChar(pos) }
  }

  def unparsed: Rule1[Option[String]] = rule {
    capture(wordBorderChar ~ ANY.*).?
  }

  def approxName: RuleNodeMeta[NamesGroup] = rule {
    uninomial ~ space ~ (approxName1 | approxName2) ~> {
      (n: NodeMeta[Name]) =>
        FactoryAST.namesGroup(n).add(warnings = Seq((3, "Name is approximate")))
    }
  }

  def approxNameIgnored: Rule1[Option[String]] = rule {
    (softSpace ~ capture(anyVisible.+ ~ (softSpace ~ anyVisible.+).*)).?
  }

  def approxName1: Rule[NodeMeta[Uninomial] :: HNil, NodeMeta[Name] :: HNil] = rule {
    approximation ~ approxNameIgnored ~> {
      (u: NodeMeta[Uninomial], appr: NodeMeta[Approximation], ign: Option[String]) =>
        FactoryAST.name(uninomial = u, approximation = appr.some, ignored = ign)
      }
  }

  def approxName2: Rule[NodeMeta[Uninomial] :: HNil, NodeMeta[Name] :: HNil] = rule {
    word ~ space ~ approximation ~ approxNameIgnored ~> {
      (u: NodeMeta[Uninomial], sw: NodeMeta[SpeciesWord],
       appr: NodeMeta[Approximation], ign: Option[String]) =>
        val nm = Name(uninomial = u.node, species = Species(sw.node).some,
                      approximation = appr.node.some, ignored = ign)
        NodeMeta(nm, u.warnings ++ sw.warnings ++ appr.warnings)
      }
  }

  def authorship: RuleNodeMeta[Authorship] = rule {
    (combinedAuthorship | basionymYearMisformed |
     basionymAuthorship | authorship1) ~ &(spaceCharsEOI ++ "(,:")
  }

  def combinedAuthorship: RuleNodeMeta[Authorship] = rule {
    combinedAuthorship1 | combinedAuthorship2
  }

  def combinedAuthorship1: RuleNodeMeta[Authorship] = rule {
    basionymAuthorship ~ authorEx ~ authorship1 ~> {
      (bauM: NodeMeta[Authorship], exM: NodeMeta[AuthorWord], exauM: NodeMeta[Authorship]) =>
        val authors1M = for { bau <- bauM; exau <- exauM }
                        yield bau.authors.copy(authorsEx = exau.authors.authors.some)
        val bau1M = for { bau <- bauM; authors1 <- authors1M; _ <- exM }
                    yield bau.copy(authors = authors1)

        bau1M.add(warnings = Seq((2, "Ex authors are not required")))
             .changeWarningsRef((bauM.node.authors, authors1M.node), (bauM.node, bau1M.node),
                                (exM.node, bau1M.node))
    }
  }

  def combinedAuthorship2: RuleNodeMeta[Authorship] = rule {
    basionymAuthorship ~ softSpace ~ authorship1 ~> {
      (bauM: NodeMeta[Authorship], cauM: NodeMeta[Authorship]) =>
        val r = for { bau <- bauM; cau <- cauM }
                yield bau.copy(combination = cau.authors.some, basionymParsed = true)
        r.changeWarningsRef((bauM.node, r.node))
    }
  }

  def basionymYearMisformed: RuleNodeMeta[Authorship] = rule {
    '(' ~ softSpace ~ authorsGroup ~ softSpace ~ ')' ~ (softSpace ~ ',').? ~ softSpace ~ year ~> {
      (aM: NodeMeta[AuthorsGroup], yM: NodeMeta[Year]) =>
        val authors1 = aM.map { a => a.copy(year = yM.node.some) }
        FactoryAST.authorship(authors = authors1, inparenthesis = true, basionymParsed = true)
          .add(warnings = Seq((2, "Misformed basionym year")))
          .changeWarningsRef((aM.node, authors1.node))
    }
  }

  def basionymAuthorship: RuleNodeMeta[Authorship] = rule {
    basionymAuthorship1 | basionymAuthorship2
  }

  def basionymAuthorship1: RuleNodeMeta[Authorship] = rule {
    '(' ~ softSpace ~ authorship1 ~ softSpace ~ ')' ~> {
      (aM: NodeMeta[Authorship]) =>
        val r = aM.map { a => a.copy(basionymParsed = true, inparenthesis = true) }
        r.changeWarningsRef((aM.node, r.node))
    }
  }

  def basionymAuthorship2: RuleNodeMeta[Authorship] = rule {
    '(' ~ softSpace ~ '(' ~ softSpace ~ authorship1 ~ softSpace ~ ')' ~ softSpace ~ ')' ~> {
      (aM: NodeMeta[Authorship]) =>
        val r = aM.map { a => a.copy(basionymParsed = true, inparenthesis = true) }
        r.add(warnings = Seq((3, "Authroship in double parentheses")))
         .changeWarningsRef((aM.node, r.node))
    }
  }

  def authorship1: RuleNodeMeta[Authorship] = rule {
    (authorsYear | authorsGroup) ~> { (a: NodeMeta[AuthorsGroup]) => FactoryAST.authorship(a) }
  }

  def authorsYear: RuleNodeMeta[AuthorsGroup] = rule {
    authorsGroup ~ softSpace ~ (',' ~ softSpace).? ~ year ~> {
      (aM: NodeMeta[AuthorsGroup], yM: NodeMeta[Year]) =>
        val a1 = for { a <- aM; y <- yM } yield a.copy(year = y.some)
        a1.changeWarningsRef((aM.node, a1.node))
    }
  }

  def authorsGroup: RuleNodeMeta[AuthorsGroup] = rule {
    authorsTeam ~ (authorEx ~ authorsTeam ~> {
      (exM: NodeMeta[AuthorWord], atM: NodeMeta[AuthorsTeam]) => atM.add(exM.rawWarnings) }).? ~> {
        (a: NodeMeta[AuthorsTeam], exAu: Option[NodeMeta[AuthorsTeam]]) =>
          val ag = FactoryAST.authorsGroup(a, exAu)
          val warns = exAu.map { _ => (2, "Ex authors are not required") }.toVector
          ag.add(warnings = warns)
    }
  }

  def authorsTeam: RuleNodeMeta[AuthorsTeam] = rule {
    author ~> { (aM: NodeMeta[Author]) => FactoryAST.authorsTeam(Seq(aM)) } ~
      zeroOrMore(authorSep ~ author ~> {
      (asM: NodeMeta[AuthorSep], auM: NodeMeta[Author]) =>
        for { au <- auM; as <- asM } yield au.copy(separator = as.some)
      } ~> { (atM: NodeMeta[AuthorsTeam], aM: NodeMeta[Author]) =>
        for (at <- atM; a <- aM) yield at.copy(authors = at.authors :+ a)
      })
  }

  def authorSep: RuleNodeMeta[AuthorSep] = rule {
    softSpace ~ capturePos("," | "&" | "and" | "et" | "apud") ~ softSpace ~> {
      (pos: CapturePosition) => FactoryAST.authorSep(pos)
    }
  }

  def authorEx: RuleNodeMeta[AuthorWord] = rule {
    softSpace ~ capturePos("ex" ~ '.'.? | "in") ~ space ~> { (pos: CapturePosition) =>
      val aw = FactoryAST.authorWord(pos)
      val warns = (input.charAt(pos.end - 1) == '.').option { (3, "`ex` ends with dot") }.toVector
      aw.add(warnings = warns)
    }
  }

  def author: RuleNodeMeta[Author] = rule {
    (author1 | author2 | unknownAuthor) ~> { (auM: NodeMeta[Author]) =>
      val warns = (auM.node.pos.end - auM.node.pos.start < 2).option { (3, "Author is too short") }
      auM.add(warnings = warns.toVector)
    }
  }

  def author1: RuleNodeMeta[Author] = rule {
    author2 ~ softSpace ~ filius ~> {
      (auM: NodeMeta[Author], filiusM: NodeMeta[AuthorWord]) =>
        val au1M = for { au <- auM; filius <- filiusM } yield au.copy(filius = filius.some)
        au1M.changeWarningsRef((auM.node, au1M.node))
    }
  }

  def author2: RuleNodeMeta[Author] = rule {
    authorWord ~ zeroOrMore(authorWordSep) ~ !':' ~> {
      (auM: NodeMeta[AuthorWord], ausM: Seq[NodeMeta[AuthorWord]]) =>
        for { au <- auM; aus <- lift(ausM) } yield Author(au +: aus)
    }
  }

  def authorWordSep: RuleNodeMeta[AuthorWord] = rule {
    capture(ch(dash) | softSpace) ~ authorWord ~> {
      (sep: String, awM: NodeMeta[AuthorWord]) =>
        val aw1M = awM.map { aw => sep match {
          case d if d.length == 1 && d(0) == dash =>
            aw.copy(separator = AuthorWordSeparator.Dash)
          case _ =>
            awM.node.copy(separator = AuthorWordSeparator.Space)
        }}
        aw1M.changeWarningsRef((awM.node, aw1M.node))
    }
  }

  def unknownAuthor: RuleNodeMeta[Author] = rule {
    capturePos("?" | (("auct" | "anon") ~ (&(spaceCharsEOI) | '.'))) ~> {
      (authPos: CapturePosition) =>
        val endsWithQuestion = input.charAt(authPos.end - 1) == '?'
        val warns = Vector((2, "Author is unknown").some,
                           endsWithQuestion.option((3, "Author as a question mark")))
        FactoryAST.author(Seq(FactoryAST.authorWord(authPos)), anon = true)
                  .add(warnings = warns.flatten)
    }
  }

  def authorWord: RuleNodeMeta[AuthorWord] = rule {
    (authorWord1 | authorWord2 | authorPrefix) ~> {
      (awM: NodeMeta[AuthorWord]) =>
        val word = input.sliceString(awM.node.pos.start, awM.node.pos.end)
        val authorIsUpperCase =
          word.length > 2 && word.forall { ch => ch == dash || authCharUpperStr.indexOf(ch) >= 0 }
        val warns = authorIsUpperCase.option { (2, "Author in upper case") }.toVector
        awM.add(warnings = warns)
    }
  }

  def authorWord1: RuleNodeMeta[AuthorWord] = rule {
    capturePos("arg." | "et al.{?}" | ("et" | "&") ~ " al" ~ '.'.?) ~> {
      (pos: CapturePosition) => FactoryAST.authorWord(pos)
    }
  }

  def authorWord2: RuleNodeMeta[AuthorWord] = rule {
    capturePos("d'".? ~ authCharUpper ~ zeroOrMore(authCharUpper | authCharLower) ~ '.'.?) ~> {
      (pos: CapturePosition) => FactoryAST.authorWord(pos)
    }
  }

  def filius: RuleNodeMeta[AuthorWord] = rule {
    capturePos("f." | "fil." | "filius") ~> { (pos: CapturePosition) => FactoryAST.authorWord(pos) }
  }

  def authorPrefix: RuleNodeMeta[AuthorWord] = rule {
    capturePos((("ab" | "af" | "bis" | "da" | "der" | "des" | "den" | "del" | "della" | "dela" |
                 "de" | "di" | "du" | "el" | "la" | "le" | "ter" | "van" |
                 ("von" ~ (space ~ "dem").?) |
                 ("v" ~ (space ~ "d").?) | "d'" | "in't") ~ &(spaceCharsEOI)) |
      ("v." ~ (space.? ~ "d.").?) | "'t") ~> {
      (pos: CapturePosition) => FactoryAST.authorWord(pos)
    }
  }

  def year: RuleNodeMeta[Year] = rule {
    yearRange | yearApprox | yearWithParens | yearWithPage | yearWithDot | yearWithChar | yearNumber
  }

  def yearRange: RuleNodeMeta[Year] = rule {
    yearNumber ~ dash ~ capturePos(oneOrMore(Digit)) ~ zeroOrMore(Alpha ++ "?") ~> {
      (yStartM: NodeMeta[Year], yEnd: CapturePosition) =>
        val yrM = yStartM.map { yStart => yStart.copy(approximate = true, rangeEnd = Some(yEnd)) }
        yrM.add(warnings = Seq((3, "Years range")))
           .changeWarningsRef((yStartM.node, yrM.node))
    }
  }

  def yearWithDot: RuleNodeMeta[Year] = rule {
    yearNumber ~ '.' ~> { (y: NodeMeta[Year]) => y.add(warnings = Seq((2, "Year with period"))) }
  }

  def yearApprox: RuleNodeMeta[Year] = rule {
    '[' ~ softSpace ~ yearNumber ~ softSpace ~ ']' ~> {
      (yM: NodeMeta[Year]) =>
        val yrM = yM.map { y => y.copy(approximate = true) }
        yrM.add(warnings = Seq((3, "Year with square brakets")))
           .changeWarningsRef((yM.node, yrM.node))
    }
  }

  def yearWithPage: RuleNodeMeta[Year] = rule {
    (yearWithChar | yearNumber) ~ softSpace ~ ':' ~ softSpace ~ oneOrMore(Digit) ~> {
      (yM: NodeMeta[Year]) => yM.add(warnings = Seq((3, "Year with page info")))
    }
  }

  def yearWithParens: RuleNodeMeta[Year] = rule {
    '(' ~ softSpace ~ (yearWithChar | yearNumber) ~ softSpace ~ ')' ~> {
      (yM: NodeMeta[Year]) =>
        val y1M = yM.map { y => y.copy(approximate = true) }
        y1M.add(warnings = Seq((2, "Year with parentheses")))
           .changeWarningsRef((yM.node, y1M.node))
      }
  }

  def yearWithChar: RuleNodeMeta[Year] = rule {
    yearNumber ~ capturePos(Alpha) ~> {
      (yM: NodeMeta[Year], pos: CapturePosition) =>
        val y1M = yM.map { y => y.copy(alpha = pos.some) }
        y1M.add(warnings = Seq((2, "Year with latin character")))
           .changeWarningsRef((yM.node, y1M.node))
    }
  }

  def yearNumber: RuleNodeMeta[Year] = rule {
    capturePos(CharPredicate("12") ~ CharPredicate("0789") ~ Digit ~ (Digit|'?') ~ '?'.?) ~> {
      (yPos: CapturePosition) =>
        val yrM = FactoryAST.year(yPos)
        if (input.charAt(yPos.end - 1) == '?') {
          val yr1M = yrM.map { yr => yr.copy(approximate = true) }
          yr1M.add(warnings = Seq((2, "Year with question mark")))
        } else yrM
    }
  }

  def softSpace: Rule0 = rule { zeroOrMore(spaceChars) }

  def space: Rule0 = rule { oneOrMore(spaceChars) }
}

object Parser {
  implicit def nodeToMeta[T <: AstNode](node: T): NodeMeta[T] = NodeMeta[T](node)

  trait NodeMetaBase[T] {
    val node: T
    val warnings: Vector[Warning]
  }

  def lift[T <: AstNode](nodeOpt: Option[NodeMeta[T]]): NodeMetaOpt[T] = nodeOpt match {
    case None => NodeMetaOpt(None)
    case Some(nodeMeta) => NodeMetaOpt(nodeMeta.node.some, nodeMeta.warnings)
  }

  def lift[T <: AstNode](nodeSeq: Seq[NodeMeta[T]]): NodeMetaSeq[T] = {
    val warns = nodeSeq.flatMap { _.warnings }.toVector
    NodeMetaSeq(nodeSeq.map { _.node }, warns)
  }

  case class NodeMetaSeq[T <: AstNode](node: Seq[T], warnings: Vector[Warning] = Vector.empty)
    extends NodeMetaBase[Seq[T]] {

    def map[M <: AstNode](f: Seq[T] => M): NodeMeta[M] = {
      val r = f(node)
      NodeMeta(r, warnings)
    }

    def flatMap[M <: AstNode](f: Seq[T] => NodeMeta[M]): NodeMeta[M] = {
      val r = f(node)
      r.copy(warnings = r.warnings ++ warnings)
    }
  }

  case class NodeMetaOpt[T <: AstNode](node: Option[T], warnings: Vector[Warning] = Vector.empty)
    extends NodeMetaBase[Option[T]] {

    def map[M <: AstNode](f: Option[T] => M): NodeMeta[M] = {
      val r = f(node)
      NodeMeta(r, warnings)
    }

    def flatMap[M <: AstNode](f: Option[T] => NodeMeta[M]): NodeMeta[M] = {
      val r = f(node)
      r.copy(warnings = r.warnings ++ warnings)
    }
  }

  case class NodeMeta[T <: AstNode](node: T, warnings: Vector[Warning] = Vector.empty)
    extends NodeMetaBase[T] {

    val rawWarnings = warnings.map { w => (w.level, w.message) }

    def changeWarningsRef(substitutions: (AstNode, AstNode)*): NodeMeta[T] = {
      val substWarnsMap = substitutions.toMap
      val ws = warnings.map { w =>
        substWarnsMap.get(w.node).map { subst => w.copy(node = subst) }.getOrElse(w)
      }
      this.copy(warnings = ws)
    }

    def add(warnings: Seq[(Int, String)] = Seq.empty): NodeMeta[T] = {
      if (warnings.isEmpty) this
      else {
        val ws =
          this.warnings ++ warnings.map { case (level, message) => Warning(level, message, node) }
        this.copy(warnings = ws)
      }
    }

    def map[M <: AstNode](f: T => M): NodeMeta[M] = {
      val node1 = f(node)
      this.copy(node = node1)
    }

    def flatMap[M <: AstNode](f: T => NodeMeta[M]): NodeMeta[M] = {
      val nodeM = f(node)
      nodeM.copy(warnings = nodeM.warnings ++ warnings)
    }
  }

  private final val digitNonZero = Digit -- "0"
  private final val dash = '-'
  private final val word2sep = CharPredicate("." + dash)
  private final val spaceMiscoded = "　 \t\r\n\f_"
  private final val spaceChars = CharPredicate(" " + spaceMiscoded)
  private final val spaceCharsEOI = spaceChars ++ EOI ++ ";"
  private final val wordBorderChar = spaceChars ++ CharPredicate(";.,:()]")
  private final val sciCharsExtended = "æœſàâåãäáçčéèíìïňññóòôøõöúùüŕřŗššşž"
  private final val sciUpperCharExtended = "ÆŒÖ"
  private final val authCharUpperStr =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝĆČĎİĶĹĺĽľŁłŅŌŐŒŘŚŜŞŠŸŹŻŽƒǾȘȚ"
  private final val authCharMiscoded = '�'
  private final val apostr = '\''
  private final val doubleSpacePattern = Pattern.compile("""[\s_]{2}""")
  private final val authCharLower = LowerAlpha ++
    "àáâãäåæçèéêëìíîïðñòóóôõöøùúûüýÿāăąćĉčďđ'-ēĕėęěğīĭİıĺľłńņňŏőœŕřśşšţťũūŭůűźżžſǎǔǧșțȳß"
  private final val authCharUpper = CharPredicate(authCharUpperStr + authCharMiscoded)
  private final val upperChar = UpperAlpha ++ "Ë" ++ sciUpperCharExtended
  private final val lowerChar = LowerAlpha ++ "ë" ++ sciCharsExtended
  private final val anyVisible = upperChar ++ lowerChar ++ CharPredicate.Visible
}
