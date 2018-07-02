package org.globalnames
package parser

import formatters._
import org.parboiled2._
import shapeless._

import scala.util.{Failure, Success}

class Result(val preprocessorResult: Preprocessor.Result,
             val scientificName: ScientificName,
             val version: String,
             val warnings: Vector[Warning] = Vector()) {
  val canonizer: Canonizer = new Canonizer(this)
  val normalizer: Normalizer = new Normalizer(this)
  val positions: Positions = new Positions(this)
}

abstract class ScientificNameParser {
  val version: String

  def fromString(input: String): Result =
    fromString(input, collectParsingErrors = false)

  def fromString(input: String,
                 collectParsingErrors: Boolean): Result = {
    val preprocessorResult = Preprocessor.process(Option(input).getOrElse(""))
    if (preprocessorResult.virus || preprocessorResult.noParse) {
      new Result(preprocessorResult, ScientificName(), version)
    } else {
      val parser = new Parser(preprocessorResult, collectParsingErrors)
      parser.sciName.run() match {
        case Success(scientificName :: warnings :: HNil) =>
          new Result(preprocessorResult, scientificName, version, warnings)
        case Failure(err: ParseError) if collectParsingErrors =>
          Console.err.println(err.format(preprocessorResult.verbatim))
          new Result(preprocessorResult, ScientificName(), version)
        case Failure(_) =>
          new Result(preprocessorResult, ScientificName(), version)
      }
    }
  }
}

object ScientificNameParser {

  final val instance = new ScientificNameParser {
    override final val version: String = BuildInfo.version
  }

}
