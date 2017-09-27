package org.globalnames
package parser

import java.util.UUID
import java.util.regex.Pattern

import shapeless._

import scala.util.matching.Regex

import scalaz._
import Scalaz._

object Preprocessor {
  case class Result(verbatim: String, unescaped: String, warnings: Seq[WarningInfo],
                    virus: Boolean, noParse: Boolean, surrogate: Option[Boolean] = None,
                    private val UNESCAPE_HTML4: TrackingPositionsUnescapeHtml4Translator) {
    val id: UUID = UuidGenerator.generate(verbatim)

    def verbatimPosAt(pos: Int): Int = UNESCAPE_HTML4.at(pos)
  }

  private final val substitutionsPatterns = {
    val notes = """(?ix)\s+(species\s+group|species\s+complex|group|author|emend)\b.*$"""
    val taxonConcepts1 = """(?i)\s+(sensu|auct|sec|near|str)\.?\b.*$"""
    val taxonConcepts2 =
      """(?x)(,\s*|\s+)
        |(\(?s\.\s?s\.|\(?s\.\s?l\.|\(?s\.\s?str\.|\(?s\.\s?lat\.).*$""".stripMargin
    val taxonConcepts3 = """(?i)(,\s*|\s+)(pro parte|p\.\s?p\.)\s*$"""
    val nomenConcepts = """(?i)(,\s*|\s+)(\(?nomen|\(?nom\.|\(?comb\.).*$"""
    val lastWordJunk = """(?ix)(,\s*|\s+)
                          (var\.?|von|van|ined\.?|
                          sensu|new|non|nec|nudum|
                          ssp\.?|subsp|subgen|hybrid|hort\.?|cf\.?)\??\s*$"""
    object PatternCompile extends Poly1 {
      implicit def default = at[String] { x => Pattern.compile(x) }
    }
    (notes :: taxonConcepts1 :: taxonConcepts2 :: taxonConcepts3 ::
      nomenConcepts :: lastWordJunk :: HNil).map { PatternCompile }
  }

  private object RemoveJunk extends Poly2 {
    implicit def default =
      at[String, Pattern] { (str, ptrn) => ptrn.matcher(str).replaceFirst("") }
  }

  private def removeJunk(input: String): String = {
    substitutionsPatterns.foldLeft(input)(RemoveJunk)
  }

  private val hybridPattern1 = Pattern.compile("""(^)[Xx](\p{Lu})""")
  private val hybridPattern2 = Pattern.compile("""(\s|^)[Xx](\s|$)""")

  private def normalizeHybridChar(input: String): String = {
    val normalizedHybridChar = "$1×$2"
    hybridPattern2.matcher(
      hybridPattern1.matcher(input).replaceAll(normalizedHybridChar)
    ).replaceAll(normalizedHybridChar)
  }

  private final val virusPatterns =
    """\sICTV\s*$""".r :: """[A-Z]?[a-z]+virus\b""".r ::
      """(?ix)\b(virus(es)?|particles?|(bacterio|viro)?phages?|
               viroids?|prions?|NPV)\b""".r ::
      """\b(alpha|beta)?satellites?\b""".r ::
      """\b[A-Za-z]*NPV\b""".r :: HNil

  private final val noParsePatterns = {
    val incertaeSedis1 = """(?i).*incertae\s+sedis.*""".r
    val incertaeSedis2 = """(?i)inc\.\s*sed\.""".r
    val phytoplasma = """(?i)phytoplasma\b""".r
    val rna = """[^A-Z]RNA[^A-Z]*""".r
    incertaeSedis1 :: incertaeSedis2 :: phytoplasma :: rna :: HNil
  }

  private def checkVirus(input: String): Boolean = {
    object PatternMatch extends Poly2 {
      implicit def default =
        at[Boolean, Regex]{ _ && _.findFirstIn(input).isEmpty }
    }
    !virusPatterns.foldLeft(true)(PatternMatch)
  }

  private def noParse(input: String): Boolean = {
    object PatternMatch extends Poly2 {
      implicit def default =
        at[Boolean, Regex]{ _ && _.findFirstIn(input).isEmpty }
    }
    !noParsePatterns.foldLeft(true)(PatternMatch)
  }

  private final val comparisonPattern = """(?ix)(,\s*|\s+)cf\.?\s*$""".r

  def process(input: String): Result = {
    val UNESCAPE_HTML4 = new TrackingPositionsUnescapeHtml4Translator

    val unescaped = UNESCAPE_HTML4.translate(input)

    val (comparisonCleaned, isComparisonRemoved) = {
      val matches = comparisonPattern.findFirstIn(unescaped).isDefined
      (matches ? comparisonPattern.replaceAllIn(unescaped, "") | unescaped, matches)
    }

    val preprocessed = normalizeHybridChar(removeJunk(comparisonCleaned))

    val isPreprocessed =
      !UNESCAPE_HTML4.identity || unescaped.length != preprocessed.length

    val warnings = (isComparisonRemoved || isPreprocessed).option {
      WarningInfo(2, "Name had to be changed by preprocessing")
    }.toVector ++ isComparisonRemoved.option {
      WarningInfo(3, "Name comparison")
    }.toVector

    Result(verbatim = input, unescaped = preprocessed, warnings = warnings,
           virus = checkVirus(input), noParse = noParse(input),
           surrogate = isComparisonRemoved.option { true },
           UNESCAPE_HTML4)
  }
}
