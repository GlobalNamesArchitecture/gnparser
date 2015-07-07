package org.globalnames.parser

import collection.mutable.{Buffer}

object Util {
  def normAuthWord(input: String): String = {
    if (input.matches("""[\p{Lu}]{3,}"""))
      input.split("-").map(_.toLowerCase.capitalize).mkString("-")
    else input
  }

  def norm(input: String): String = {
    var output: Buffer[Char] = Buffer()
    val charFrom = "ÀÂÅÃÄÁÇČËÉÈÍÌÏŇÑÑÓÒÔØÕÖÚÙÜŔŘŖŠŠŞŽ" +
                   "àâåãäáçčëéèíìïňññóòôøõöúùüŕřŗſššşž"
    val charTo   = "AAAAAACCEEEIIINNNOOOOOOUUURRRSSSZ" +
                   "aaaaaacceeeiiinnnoooooouuurrrssssz"

    for (chr <- input) {
      val index = charFrom.indexOf(chr)
      chr match {
        case 'Æ' => "Ae".foreach(output += _)
        case 'Œ' => "Oe".foreach(output += _)
        case 'æ' => "ae".foreach(output += _)
        case 'œ' => "oe".foreach(output += _)
        case '\'' =>
        case _ => output += (if (index > -1) charTo(index) else chr)
      }
    }
    val res = output.mkString
    res.replaceFirst("""\?$""", "") //remove question mark from the end
  }
}
