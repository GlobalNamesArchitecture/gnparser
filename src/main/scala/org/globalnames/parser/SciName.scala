package org.globalnames.parser

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.apache.commons.id.uuid.UUID
import scala.collection._
import scala.util.{Success, Failure}

case class SciName(
  verbatim: String,
  normalized: Option[String] = None,
  canonical: Option[String] = None,
  isParsed: Boolean = false,
  isVirus: Boolean = false,
  isHybrid: Boolean = false,
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
    ("verbatim" -> verbatim) ~
    ("normalized" -> normalized.getOrElse(null)) ~
    ("canonical" -> canonical.getOrElse(null)) ~
    ("parsed" -> isParsed) ~
    ("hybrid" -> isHybrid) ~
    ("virus" -> isVirus) ~
    ("parser_version" -> parserVersion))

}

object SciName {
  def fromString(input: String): SciName = {
    val pc = new ParserClean(input)
    val parsed = pc.sciName.run()
    parsed match {
      case Success(res: SciName) => res
      case Failure(err) => {
        println(err)
          SciName(input)
      }
      case _ => SciName(input)
    }
  }
}
