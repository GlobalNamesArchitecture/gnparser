package org.globalnames.parser

import collection.mutable.{Buffer}

object Util {
  def norm(input: String): String = {
    var output: Buffer[Char] = Buffer()
    val charFrom = "ÀÂÅÃÄÁÇČËÉÈÍÌÏŇÑÑÓÒÔØÕÖÚÙÜŔŘŖŠŠŞŽ" +
                   "àâåãäáçčëéèíìïňññóòôøõöúùüŕřŗššşž"
    val charTo   = "AAAAAACCEEEIIINNNOOOOOOUUURRRSSSZ" +
                   "aaaaaacceeeiiinnnoooooouuurrrsssz"

    for (chr <- input) {
      val index = charFrom.indexOf(chr)
      chr match {
        case 'Æ' => "AE".foreach(output += _)
        case 'Œ' => "OE".foreach(output += _)
        case 'æ' => "ae".foreach(output += _)
        case 'œ' => "oe".foreach(output += _)
        case '\'' =>
        case _ => output += (if (index > -1) charTo(index) else chr)
      }
    }
    output.mkString
  }
}
