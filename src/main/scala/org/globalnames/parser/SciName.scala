package org.globalnames.parser

import scala.collection._

class SciName(nameString: String) {
  val verbatim = nameString
  var isParsed = false
  var isVirus = false
  var isHybrid = false
  var canonical: String = null

  def toJson: String = s"""{\"verbatim\": \"${this.verbatim}\"}"""
}

object SciName {
  def apply(input: String) = new SciName(input)
}
