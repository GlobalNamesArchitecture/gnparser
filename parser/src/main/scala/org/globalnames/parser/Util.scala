package org.globalnames.parser

import collection.mutable.{Buffer}

object Util {
  def normAuthWord(input: String): String = {
    if (input.matches("""[\p{Lu}]{3,}[\p{Lu}-]*"""))
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
    val res = output.mkString.replaceFirst("""\?$""", "")
    if (res(0).isDigit) numsToString(res) else res
  }

  private def numsToString(input: String): String = {
    val (numeral, suffix) = input.splitAt(input.indexOf('-') + 1)
    val prefix = numeral match {
        case "1-"  => "uni"
        case "2-"  => "bi"
        case "3-"  => "tri"
        case "4-"  => "quadri"
        case "5-"  => "quinque"
        case "6-"  => "sex"
        case "7-"  => "septem"
        case "8-"  => "octo"
        case "9-"  => "novem"
        case "10-" => "decem"
        case "11-" => "undecim"
        case "12-" => "duodecim"
        case "13-" => "tredecim"
        case "14-" => "quatuordecim"
        case "15-" => "quindecim"
        case "16-" => "sedecim"
        case "17-" => "septendecim"
        case "18-" => "octodecim"
        case "19-" => "novemdecim"
        case "20-" => "viginti"
        case "21-" => "vigintiuno"
        case "22-" => "vigintiduo"
        case "23-" => "vigintitre"
        case "24-" => "vigintiquatuor"
        case "25-" => "vigintiquinque"
        case "26-" => "vigintisex"
        case "27-" => "vigintiseptem"
        case "28-" => "vigintiocto"
        case "30-" => "triginta"
        case "31-" => "trigintauno"
        case "32-" => "trigintaduo"
        case "38-" => "trigintaocto"
        case "40-" => "quadraginta"
        case _     => numeral
      }
    prefix + suffix
  }
}
