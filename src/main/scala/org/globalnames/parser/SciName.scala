package org.globalnames.parser

import org.apache.commons.id.uuid.UUID
import org.apache.commons.lang.StringEscapeUtils
import org.json4s.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.parboiled2._

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

case class SciName(
  verbatim: String = "",
  ast: Option[NamesGroup] = None,
  isVirus: Boolean = false
) {

  lazy val node: Node = ast match {
    case _ => Node()
  }

  val id: String = {
    val gn = UUID.fromString("90181196-fecf-5082-a4c1-411d4f314cda")
    val uuid = UUID.nameUUIDFromString(verbatim, gn, "SHA1").toString
    s"${uuid.substring(0, 14)}5${uuid.substring(15, uuid.length)}"
  }

  val json: JValue = render("scientificName" ->
    ("id" -> id) ~
      ("parsed" -> node.isParsed) ~
      ("parser_version" -> SciName.parserVersion) ~
      ("verbatim" -> verbatim) ~
      ("normalized" -> node.normalized.orNull) ~
      ("canonical" -> node.canonical.orNull) ~
      ("hybrid" -> node.isHybrid) ~
      ("virus" -> isVirus))

  val renderCompactJson: String = compact(json)
}

object SciName {
  private val parserClean = new ParserClean()

  val parserVersion: String = BuildInfo.version

  def fromString(input: String): SciName = {
    val isVirus = detectVirus(input)
    if (isVirus || noParse(input)) SciName(input, isVirus = isVirus)
    else {
      val parserInput = preprocess(input)
      parse(input, parserInput)
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


  def processParsed(input: String, parser: Parser,
                    result: Try[SciName]): SciName = {
    result match {
      case Success(res: SciName) => res.copy(input)
      case Failure(err: ParseError) => {
        println(err.format(input))
        SciName(input)
      }
      case Failure(err) => {
        //println(err)
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

  private def parse(input: String, parserInput: String): SciName = {
    val res =  parserClean.sciName.run(parserInput)
    processParsed(input, parserClean, res)
  }

  private def preprocess(input: String): String = {
    val unescaped = StringEscapeUtils.unescapeHtml(input)
    val unjunk = removeJunk(unescaped)
    val authPre = prependAuthorPre(unjunk)
    val normHybrids = normalizeHybridChar(authPre)
    parserSpaces(normHybrids)
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

  private def prependAuthorPre(input: String): String = {
    val authPre = """(?x)([^�-])\b(ab|af|bis|da|der|des|den|della|dela|
      de|di|du|la|ter|van|von)\b"""
    val prefix = """\b(d'\p{Lu})"""
    input.replaceAll(authPre, "$1ж$2").replaceAll(prefix, "ж$1")
  }

  private def normalizeHybridChar(input: String): String = {
    input.replaceAll(" [Xx] ", " × ")
      .replaceAll("""^\s*[Xx]\s*([\p{Lu}])""", "× $1")
  }

  private def parserSpaces(input: String): String = {
    val res1 = input.replaceAll("""([^\sщ])([:&\(\)\[\],×])""", "$1щ$2")
    res1.replaceAll("""([:&\.\)\(\[\],×])([^\sщ])""", "$1щ$2")
  }
}
