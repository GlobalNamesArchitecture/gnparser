package org.globalnames.parser

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.apache.commons.id.uuid.UUID
import org.parboiled2._
import scala.collection._
import scala.util.{Success, Failure, Try}
import org.apache.commons.lang.StringEscapeUtils
import scala.util.matching.Regex

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
    ("hybrid" -> isHybrid) ~
    ("virus" -> isVirus)
  )
}

object SciName {
  def fromString(input: String): SciName = {
    val isVirus = detectVirus(input)
    if (isVirus || noParse(input)) SciName(input, isVirus = isVirus)
    else {
      val (parserInput, parserRun) = preprocess(input)
      parse(input, parserInput, parserRun)
    }
  }

  private def detectVirus(input: String): Boolean = {
    val vir1 = """\sICTV\s*$""".r
    val vir2 = """(?ix)\b(virus|viruses|particle|particles|
                          phage|phages|viroid|viroids|virophage|
                          prion|prions|NPV)\b""".r
    val vir3 = """[A-Z]?[a-z]+virus\b""".r
    val vir4 = """\b[A-Za-z]*(satellite[s]?|NPV)\b""".r
    !(List(vir1, vir2, vir3, vir4).foldLeft(true){ (b: Boolean, r: Regex) =>
      (r.findFirstIn(input) == None) && b })
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

  private def noParse(input: String): Boolean = {
    val incertaeSedis1 = """(?i).*incertae\s+sedis.*""".r
    val incertaeSedis2 = """(?i)inc\.\s*sed\.""".r
    val rna = """[^A-Z]RNA[^A-Z]*""".r
    if (List(incertaeSedis1.findFirstIn(input),
      incertaeSedis2.findFirstIn(input),
      rna.findFirstIn(input)) == List(None, None, None)) false
    else true
  }

  private def parse(input: String, parserInput: String,
    parserRun: Int): SciName = {
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

  private def preprocess(input: String): (String, Int) = {
    var parserRun = 1
    val unescaped = StringEscapeUtils.unescapeHtml(input)
    val unquoted = unescaped.replaceAllLiterally("\"", "")
    val unsensu = unquoted.replaceFirst("""\s+(sec\.|sensu\.).*$""", "")
    val unjunk = removeJunk(unsensu)
    if (unescaped != input || unquoted != unescaped) parserRun = 2
    (parserSpaces(unjunk), parserRun)
  }

  private def removeJunk(input: String): String = {
    val notes = """(?ix)\s+(species\s+group|
                   species\s+complex|group|author)\b.*$"""
    val taxonConcepts1 = """(?i)\s+(sensu\.|sensu|auct\.|auct)\b.*$"""
    val taxonConcepts2 = """(?x)\s+
                       (\(?s\.\s?s\.|
                       \(?s\.\s?l\.|
                       \(?s\.\s?str\.|
                       \(?s\.\s?lat\.|
                      sec\.|sec|near)\b.*$"""
    val taxonConcepts3 = """(?i)(,\s*|\s+)(pro parte|p\.\s?p\.)\s*$"""
    val nomenConcepts  = """(?i)(,\s*|\s+)(\(?nomen|\(?nom\.|\(?comb\.).*$"""
    val lastWordJunk  = """(?ix)(,\s*|\s+)
                    (spp\.|spp|var\.|
                     var|von|van|ined\.|
                     ined|sensu|new|non|nec|
                     nudum|cf\.|cf|sp\.|sp|
                     ssp\.|ssp|subsp|subgen|hybrid|hort\.|hort)\??\s*$"""
    substitute(input, List(notes, taxonConcepts1,
      taxonConcepts2, taxonConcepts3, nomenConcepts, lastWordJunk))

  }

  @annotation.tailrec
  def substitute(input: String, regexes: List[String]): String = {
    if (regexes == List()) input
    else substitute(input.replaceFirst(regexes.head, ""), regexes.tail)
  }

  private def parserSpaces(input: String): String = {
    val res1 = input.replaceAll("""([^\sщ])([&\(\)\[\],])""", "$1щ$2")
    res1.replaceAll("""([&\.\)\(\[\],])([^\sщ])""", "$1щ$2")
  }
}
