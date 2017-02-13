package org.globalnames
package parser

import formatters._
import org.parboiled2._
import shapeless._

import scala.util.{Failure, Success}

abstract class ScientificNameParser {
  import ScientificNameParser.Result

  val version: String

  def fromString(input: String): Result =
    fromString(input, collectParsingErrors = false)

  def fromString(input: String,
                 collectParsingErrors: Boolean): Result = {
    val preprocessorResult = Preprocessor.process(input)
    if (preprocessorResult.virus || preprocessorResult.noParse) {
      Result(preprocessorResult, ScientificName(), version)
    } else {
      val parser = new Parser(preprocessorResult.unescaped,
                              preprocessorResult.preprocessed, collectParsingErrors)
      parser.sciName.run() match {
        case Success(scientificName :: warnings :: HNil) =>
          Result(preprocessorResult, scientificName, version, warnings)
        case Failure(err: ParseError) if collectParsingErrors =>
          Console.err.println(err.format(preprocessorResult.verbatim))
          Result(preprocessorResult, ScientificName(), version)
        case Failure(err) =>
          Result(preprocessorResult, ScientificName(), version)
      }
    }
  }
}

object ScientificNameParser {

  final val instance = new ScientificNameParser {
    override final val version: String = BuildInfo.version
  }

  case class Result(preprocessorResult: Preprocessor.Result, scientificName: ScientificName,
                    version: String, warnings: Vector[Warning] = Vector.empty)
    extends JsonRenderer with DelimitedStringRenderer with Details
       with Positions with Normalizer with Canonizer {

    private[parser] def stringOf(astNode: AstNode): String =
      preprocessorResult.unescaped.substring(astNode.pos.start, astNode.pos.end)

    private[parser] def namesEqual(name1: Name, name2: Name): Boolean = {
      val name1str = stringOf(name1)
      val name2str = stringOf(name2)
      !name1.uninomial.implied && !name2.uninomial.implied &&
        (name1str.startsWith(name2str) ||
        (name2str.endsWith(".") && name1str.startsWith(name2str.substring(0, name2str.length - 1))))
    }
  }
}
