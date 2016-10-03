package org.globalnames
package parser

import java.util.UUID
import java.util.regex.Pattern

import org.globalnames.parser.formatters._
import org.parboiled2._
import shapeless._

import scala.util.matching.Regex
import scala.util.{Failure, Success}

abstract class ScientificNameParser {
  import ScientificNameParser.{Input, Result}

  val version: String

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

  def fromString(input: String): Result =
    fromString(input, collectParsingErrors = false)

  def fromString(input: String,
                 collectParsingErrors: Boolean): Result = {
    val isVirus = checkVirus(input)
    val inputString = Input(input)
    if (isVirus || noParse(input)) {
      Result(inputString, ScientificName(isVirus = isVirus), version)
    } else {
      val input = inputString.unescaped
      val parser = new Parser(input, inputString.preprocessed,
                              collectParsingErrors)
      parser.sciName.run() match {
        case Success(scientificName :: warnings :: HNil) =>
          Result(inputString, scientificName, version, warnings)
        case Failure(err: ParseError) if collectParsingErrors =>
          Console.err.println(err.format(inputString.verbatim))
          Result(inputString, ScientificName(), version)
        case Failure(err) =>
          Result(inputString, ScientificName(), version)
      }
    }
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
}

object ScientificNameParser {
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
                    (var\.|var|von|van|ined\.|
                     ined|sensu|new|non|nec|nudum|
                     ssp\.|ssp|subsp|subgen|hybrid|hort\.|hort)\??\s*$"""
    object PatternCompile extends Poly1 {
      implicit def default = at[String] { x => Pattern.compile(x) }
    }
    (notes :: taxonConcepts1 :: taxonConcepts2 :: taxonConcepts3 ::
      nomenConcepts :: lastWordJunk :: HNil).map { PatternCompile }
  }

  val uuidGenerator = UuidGenerator()

  final val instance = new ScientificNameParser {
    override final val version: String = BuildInfo.version
  }

  case class Result(input: Input, scientificName: ScientificName,
                    version: String, warnings: Vector[Warning] = Vector.empty)
    extends JsonRenderer with DelimitedStringRenderer with Details
       with Positions with Normalizer with Canonizer {

    def stringOf(astNode: AstNode): String =
      input.unescaped.substring(astNode.pos.start, astNode.pos.end)
  }

  case class Input(verbatim: String) {
    private val UNESCAPE_HTML4 =
      new TrackingPositionsUnescapeHtml4Translator

    val (unescaped, preprocessed): (String, Boolean) = {
      val unescaped = UNESCAPE_HTML4.translate(verbatim)
      val preprocessed = normalizeHybridChar(removeJunk(unescaped))

      val isPreprocessed =
        !UNESCAPE_HTML4.identity || unescaped.length != preprocessed.length
      (preprocessed, isPreprocessed)
    }

    val id: UUID = uuidGenerator.generate(verbatim)

    def verbatimPosAt(pos: Int): Int = UNESCAPE_HTML4.at(pos)
  }

  private object RemoveJunk extends Poly2 {
    implicit def default =
      at[String, Pattern]{ (str, ptrn) => ptrn.matcher(str).replaceFirst("") }
  }

  def removeJunk(input: String): String = {
    substitutionsPatterns.foldLeft(input)(RemoveJunk)
  }

  private val hybridPattern = Pattern.compile("""(^)[Xx](\p{Lu})|(\b)[Xx](\b)""")

  def normalizeHybridChar(input: String): String = {
    hybridPattern.matcher(input).replaceAll("$1×$2")
  }
}
