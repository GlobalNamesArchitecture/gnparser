package org.globalnames
package parser

import java.util.UUID
import java.util.regex.Pattern

import shapeless._

import scala.util.matching.Regex

object Preprocessor {
  case class Result(verbatim: String, unescaped: String,
                    preprocessed: Boolean, virus: Boolean, noParse: Boolean,
                    private val UNESCAPE_HTML4: TrackingPositionsUnescapeHtml4Translator) {
    val id: UUID = UuidGenerator.generate(verbatim)

    def verbatimPosAt(pos: Int): Int = UNESCAPE_HTML4.at(pos)
  }

  private final val substitutionsPatterns = {
    val notes = """(?ix)\s+(species\s+group|
                   species\s+complex|group|author)\b.*$"""
    val taxonConcepts1 = """(?i)\s+(sensu|auct|sec|near)\.?\b.*$"""
    val taxonConcepts2 = """(?x)(,\s*|\s+)
                       (\(?s\.\s?s\.|
                       \(?s\.\s?l\.|
                       \(?s\.\s?str\.|
                       \(?s\.\s?lat\.).*$"""
    val taxonConcepts3 = """(?i)(,\s*|\s+)(pro parte|p\.\s?p\.)\s*$"""
    val nomenConcepts  = """(?i)(,\s*|\s+)(\(?nomen|\(?nom\.|\(?comb\.).*$"""
    val lastWordJunk  = """(?ix)(,\s*|\s+)
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
  private val hybridPattern2 = Pattern.compile("""(\b)[Xx](\s|$)""")

  private def normalizeHybridChar(input: String): String = {
    val normalizedHybridChar = "$1×$2"
    hybridPattern2.matcher(
      hybridPattern1.matcher(input).replaceAll(normalizedHybridChar)
    ).replaceAll(normalizedHybridChar)
  }

  private final val virusPatterns =
    """\sICTV\s*$""".r :: """[A-Z]?[a-z]+virus\b""".r ::
      """(?ix)\b(virus|viruses|particle|particles|
               phage|phages|viroid|viroids|virophage|
               prion|prions|NPV)\b""".r ::
      """\b[A-Za-z]*(satellite[s]?|NPV)\b""".r :: HNil

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

  def process(input: String): Result = {
    val UNESCAPE_HTML4 = new TrackingPositionsUnescapeHtml4Translator

    val unescaped = UNESCAPE_HTML4.translate(input)
    val preprocessed = normalizeHybridChar(removeJunk(unescaped))

    val isPreprocessed =
      !UNESCAPE_HTML4.identity || unescaped.length != preprocessed.length

    Result(verbatim = input, unescaped = preprocessed,
           preprocessed = isPreprocessed,
           virus = checkVirus(input),
           noParse = noParse(input),
           UNESCAPE_HTML4)
  }
}
