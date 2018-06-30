package org.globalnames
package parser

import formatters._
import org.json4s.JsonAST.JValue
import org.parboiled2._
import shapeless._

import scala.util.{Failure, Success}

class Result(val preprocessorResult: Preprocessor.Result,
             val scientificName: ScientificName,
             val version: String,
             val warnings: Vector[Warning] = Vector())

class ResultFat(val result: Result) {

  val canonizer: Canonizer = new Canonizer(result)
  val normalizer: Normalizer = new Normalizer(result)
  val details: Details = new Details(result, normalizer)
  val positions: Positions = new Positions(result)
  val jsonRenderer: JsonRenderer =
    new JsonRenderer(result, result.version, canonizer, normalizer, positions, details)
  val delimitedStringRenderer: DelimitedStringRenderer =
    new DelimitedStringRenderer(result, canonizer, normalizer)

  def json(showCanonicalUuid: Boolean = false): JValue =
    jsonRenderer.json(showCanonicalUuid)

  def delimitedString(delimiter: String = "\t"): String =
    delimitedStringRenderer.delimitedString(delimiter)
}

object ResultFat {
  def apply(result: Result): ResultFat = {
    new ResultFat(result)
  }
}

abstract class ScientificNameParser {
  val version: String

  def fromString(input: String): ResultFat =
    fromString(input, collectParsingErrors = false)

  def fromString(input: String,
                 collectParsingErrors: Boolean): ResultFat = {
    val preprocessorResult = Preprocessor.process(Option(input).getOrElse(""))
    val result =
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
    ResultFat(result)
  }
}

object ScientificNameParser {

  final val instance = new ScientificNameParser {
    override final val version: String = BuildInfo.version
  }
}
