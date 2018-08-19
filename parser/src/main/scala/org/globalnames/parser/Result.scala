package org.globalnames
package parser

import formatters._
import org.parboiled2._
import shapeless._

import scala.util.{Failure, Success}

case class Result(preprocessorResult: Preprocessor.Result,
                  scientificName: ScientificName,
                  warnings: Vector[Warning]) extends ResultOps with Normalizer with Canonizer

object Result {
  private def composeResult(preprocessorResult: Preprocessor.Result,
                            scientificName: ScientificName,
                            warnings: Vector[Warning] = Vector()): Result = {
    Result(preprocessorResult, scientificName, warnings)
  }

  def fromString(input: String): Result =
    fromString(input, collectParsingErrors = false)

  def fromString(input: String,
                 collectParsingErrors: Boolean): Result = {
    val preprocessorResult = Preprocessor.process(Option(input).getOrElse(""))
    if (preprocessorResult.virus || preprocessorResult.noParse) {
      composeResult(preprocessorResult, ScientificName())
    } else {
      val parser = new Parser(preprocessorResult, collectParsingErrors)
      parser.sciName.run() match {
        case Success(scientificName :: warnings :: HNil) =>
          composeResult(preprocessorResult, scientificName, warnings)

        case Failure(err: ParseError) if collectParsingErrors =>
          Console.err.println(err.format(preprocessorResult.verbatim))
          composeResult(preprocessorResult, ScientificName())

        case Failure(_) =>
          composeResult(preprocessorResult, ScientificName())
      }
    }
  }
}
