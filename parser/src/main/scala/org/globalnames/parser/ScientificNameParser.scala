package org.globalnames.parser

import org.apache.commons.id.uuid.UUID
import org.globalnames.formatters.{Canonizer, Details, Normalizer, Positions}
import org.json4s.JsonAST.{JArray, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.parboiled2._
import shapeless._

import scala.util.matching.Regex
import scala.util.{Failure, Success}

import scalaz._
import Scalaz._

abstract class ScientificNameParser {
  import ScientificNameParser.{Input, Result}

  val version: String

  private final val virusPatterns =
    """\sICTV\s*$""".r :: """[A-Z]?[a-z]+virus\b""".r ::
    """(?ix)\b(virus|viruses|particle|particles|
               phage|phages|viroid|viroids|virophage|
               prion|prions|NPV)\b""".r ::
    """\b[A-Za-z]*(satellite[s]?|NPV)\b""".r :: HNil

  def json(parserResult: Result): JValue = {
    val canonical = parserResult.canonized(showRanks = false)
    val quality = canonical.map { _ => parserResult.scientificName.quality }
    val parsed = canonical.isDefined
    val qualityWarnings: Option[JArray] =
      if (parserResult.warnings.isEmpty) None
      else {
        val warningsJArr: JArray =
          parserResult.warnings.sorted
                      .map { w => JArray(List(w.level, w.message)) }.distinct
        warningsJArr.some
      }
    val positionsJson: Option[JArray] = parsed.option {
      parserResult.positioned.map { position =>
        JArray(List(position.nodeName,
          parserResult.input.verbatimPosAt(position.start),
          parserResult.input.verbatimPosAt(position.end)))
      }
    }
    val garbage = if (parserResult.scientificName.garbage.isEmpty) None
                  else parserResult.scientificName.garbage.some

    render("scientific_name" -> ("id" -> parserResult.input.id) ~
      ("parsed" -> parsed) ~
      ("quality" -> quality) ~
      ("quality_warnings" -> qualityWarnings) ~
      ("parser_version" -> version) ~
      ("verbatim" -> parserResult.input.verbatim) ~
      ("normalized" -> parserResult.normalized) ~
      ("canonical" -> canonical) ~
      ("canonical_extended" -> parserResult.canonized(showRanks = true)) ~
      ("hybrid" -> parserResult.scientificName.isHybrid) ~
      ("surrogate" -> parserResult.scientificName.surrogate) ~
      ("garbage" -> garbage) ~
      ("virus" -> parserResult.scientificName.isVirus) ~
      ("details" -> parserResult.detailed) ~
      ("positions" -> positionsJson))
  }

  def renderCompactJson(parserResult: Result): String =
    compact(json(parserResult))

  def fromString(input: String): Result = {
    val isVirus = checkVirus(input)
    val inputString = Input(input)
    if (isVirus || noParse(input)) {
      Result(inputString, ScientificName(isVirus = isVirus))
    } else {
      val input = inputString.unescaped
      val ctx = new Parser.Context(inputString.preprocessed)
      Parser.sciName.runWithContext(input, ctx) match {
        case Success(scientificName :: warnings :: HNil) =>
          Result(inputString, scientificName, warnings)
        case Failure(err: ParseError) =>
          println(err.format(inputString.verbatim))
          Result(inputString, ScientificName())
        case Failure(err) =>
          Result(inputString, ScientificName())
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
    val incertaeSedis1 = """(?i).*incertae\s+sedis.*""".r
    val incertaeSedis2 = """(?i)inc\.\s*sed\.""".r
    val rna = """[^A-Z]RNA[^A-Z]*""".r
    if (List(incertaeSedis1.findFirstIn(input),
      incertaeSedis2.findFirstIn(input),
      rna.findFirstIn(input)) == List(None, None, None)) false
    else true
  }
}

object ScientificNameParser {
  final val instance = new ScientificNameParser {
    override final val version: String = BuildInfo.version
  }

  case class Result(input: Input, scientificName: ScientificName,
                    warnings: Vector[Warning] = Vector.empty)
    extends Details with Positions with Normalizer with Canonizer {

    def stringOf(astNode: AstNode): String =
      input.unescaped.substring(astNode.pos.start, astNode.pos.end)
  }

  case class Input(verbatim: String) {
    private lazy val UNESCAPE_HTML4 =
      new TrackingPositionsUnescapeHtml4Translator
    lazy val (unescaped, preprocessed): (String, Boolean) = {
      val unescaped = UNESCAPE_HTML4.translate(verbatim)
      val preprocessed = normalizeHybridChar(removeJunk(unescaped))

      val isPreprocessed =
        !UNESCAPE_HTML4.identity || unescaped.length != preprocessed.length
      (preprocessed, isPreprocessed)
    }

    val id: String = {
      val gn = UUID.fromString("90181196-fecf-5082-a4c1-411d4f314cda")
      val uuid = UUID.nameUUIDFromString(verbatim, gn, "SHA1").toString
      s"${uuid.substring(0, 14)}5${uuid.substring(15, uuid.length)}"
    }

    def verbatimPosAt(pos: Int): Int = UNESCAPE_HTML4.at(pos)
  }

  @annotation.tailrec
  private def substitute(input: String, regexes: List[String]): String = {
    if (regexes == List()) input
    else substitute(input.replaceFirst(regexes.head, ""), regexes.tail)
  }

  def removeJunk(input: String): String = {
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
    substitute(input, List(notes, taxonConcepts1,
      taxonConcepts2, taxonConcepts3, nomenConcepts, lastWordJunk))
  }

  def normalizeHybridChar(input: String): String = {
    input.replaceAll("""(^)[Xx](\p{Lu})|(\b)[Xx](\b)""", "$1×$2")
  }
}
