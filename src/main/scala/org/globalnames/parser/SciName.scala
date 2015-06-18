package org.globalnames.parser

import org.apache.commons.id.uuid.UUID
import scala.collection._

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
    uuid.substring(0, 14) ++ "5" ++ uuid.substring(15, uuid.length)
  }

  private val gn = UUID.fromString("90181196-fecf-5082-a4c1-411d4f314cda")
}
