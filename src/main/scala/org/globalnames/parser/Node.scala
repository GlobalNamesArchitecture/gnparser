package org.globalnames.parser
import scala.collection._

case class Node(
  normalized: String = "",
  canonical: String = "",
  hybrid: Boolean = false
)
