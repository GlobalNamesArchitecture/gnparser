package org.globalnames.parser

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.apache.commons.id.uuid.UUID
import org.parboiled2._
import scala.collection._
import scala.util.{Success, Failure, Try}
import org.apache.commons.lang.StringEscapeUtils

case class SciName(
  verbatim: String,
  normalized: Option[String] = None,
  canonical: Option[String] = None,
  isParsed: Boolean = false,
  isVirus: Boolean = false,
  isHybrid: Boolean = false,
  parserRun: Int = -1,
  parserVersion: String = BuildInfo.version
) {

  def id: String = {
    val uuid = UUID.nameUUIDFromString(verbatim, gn, "SHA1").toString
    s"${uuid.substring(0, 14)}5${uuid.substring(15, uuid.length)}"
  }

  def toJson: String = compact(render(toMap))

  private val gn = UUID.fromString("90181196-fecf-5082-a4c1-411d4f314cda")

  private val toMap = ("scientificName" ->
    ("id" -> id) ~
    ("parsed" -> isParsed) ~
    ("parser_version" -> parserVersion) ~
    ("parser_run" -> parserRun) ~
    ("verbatim" -> verbatim) ~
    ("normalized" -> normalized.getOrElse(null)) ~
    ("canonical" -> canonical.getOrElse(null)) ~
    ("hybrid" -> isHybrid)
  )
}

object SciName {
  def fromString(input: String): SciName = {
    val (parserInput, parserRun) = preprocess(input)
    println(s"***'$parserInput': '$input'")
    val parserClean = new ParserClean(parserInput)
    val resClean = parserClean.sciName.run()
    resClean match {
      case Success(_) => processParsed(input, parserClean, resClean)
      case Failure(_) => {
        val parserRelaxed = new ParserRelaxed(parserInput)
        val resRelaxed = parserRelaxed.sciName.run()
        processParsed(input, parserRelaxed, resRelaxed)
      }
    }
  }

  def processParsed(input: String,
    parser: Parser,
    result: Try[SciName]): SciName = {
    result match {
      case Success(res: SciName) => res.copy(input)
      case Failure(err: ParseError) => {
        println(parser.formatError(err))
          SciName(input)
      }
      case Failure(err) => {
        println(err)
        SciName(input)
      }
      case _ => SciName(input)
    }
  }

  private def preprocess(input: String): (String, Int) = {
    var parserRun = 1
    val unescaped = StringEscapeUtils.unescapeHtml(input)
    val unquoted = unescaped.replaceAllLiterally("\"", "")
    val unsensu = unquoted.replaceFirst("""\s+(sec\.|sensu\.).*$""", "")
    if (unescaped != input || unquoted != unescaped) parserRun = 2
    (parserSpaces(unsensu), parserRun)
  }

  private def parserSpaces(input: String): String = {
    val res1 = input.replaceAll("""([^\sщ])([&\(\),])""", "$1щ$2")
    res1.replaceAll("""([&\.\)\(,])([^\sщ])""", "$1щ$2")
  }
}
