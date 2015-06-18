package org.globalnames.parser

import scala.collection._

case class SciName(
  verbatim: String,
  normalized: Option[String] = None,
  canonical: Option[String] = None,
  isParsed: Boolean = false,
  isVirus: Boolean = false,
  isHybrid: Boolean = false
)
