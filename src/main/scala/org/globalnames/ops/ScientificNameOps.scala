package org.globalnames

import org.apache.commons.id.uuid.UUID
import org.globalnames.formatters.{Canonizer, Details, Normalizer}
import org.globalnames.parser.ScientificName
import org.json4s.{JNothing, JValue}

package object ops {

  implicit class ScientificNameOps(val underlying: ScientificName) extends AnyVal {
    def id: String = {
      val gn = UUID.fromString("90181196-fecf-5082-a4c1-411d4f314cda")
      val uuid = UUID.nameUUIDFromString(underlying.verbatim, gn, "SHA1").toString
      s"${uuid.substring(0, 14)}5${uuid.substring(15, uuid.length)}"
    }

    def canonical: Option[String] = Canonizer.format(underlying)
    def normal: Option[String] = Normalizer.format(underlying)
    def details: JValue = Details.format(underlying).removeField { case (_, v) => v == JNothing }
  }

}
