package org.globalnames.parser

object Util {
  private val charMapping: Map[Char, String] = {
    val charFrom = "ÀÂÅÃÄÁÇČËÉÈÍÌÏŇÑÑÓÒÔØÕÖÚÙÜŔŘŖŠŠŞŽàâåãäáçčëéèíìïňññóòôøõöúùüŕřŗſššşž"
    val charTo   = "AAAAAACCEEEIIINNNOOOOOOUUURRRSSSZaaaaaacceeeiiinnnoooooouuurrrssssz"
    assert(charFrom.length == charTo.length)
    val mapping = charFrom.zip(charTo.map { _.toString }).toMap
                          .updated('Æ', "Ae")
                          .updated('Œ', "Oe")
                          .updated('æ', "ae")
                          .updated('œ', "oe")
                          .updated('Ö', "Oe")
                          .updated('\'', "")
    mapping
  }

  def normalizeAuthorWord(input: String): String = {
    if (input.matches("""[\p{Lu}]{3,}[\p{Lu}-]*""")) {
      input.split("-").map { _.toLowerCase.capitalize }.mkString("-")
    } else if (input.matches("""& al\.?""")) {
      input.replace("&", "et")
    } else {
      input
    }
  }

  def normalize(input: String): String = {
    val output = new StringBuilder()
    var inputIdx = 0
    while (inputIdx < input.length) {
      val chr = input.charAt(inputIdx)
      if (inputIdx == 0 && chr.isDigit) {
        while (input.charAt(inputIdx).isDigit) {
          inputIdx += 1
        }
        output.append(numberToString(input.substring(0, inputIdx)))
        if (Set('.', '-').contains(input.charAt(inputIdx))) {
          inputIdx += 1
        }
      } else {
        if (charMapping.contains(chr)) {
          output.append(charMapping(chr))
        } else {
          output.append(chr)
        }
        inputIdx += 1
      }
    }
    val res = output.toString.replaceFirst("""\?$""", "")
    res
  }

  private def numberToString(numeral: String): String = numeral match {
    case "1"  => "uni"
    case "2"  => "bi"
    case "3"  => "tri"
    case "4"  => "quadri"
    case "5"  => "quinque"
    case "6"  => "sex"
    case "7"  => "septem"
    case "8"  => "octo"
    case "9"  => "novem"
    case "10" => "decem"
    case "11" => "undecim"
    case "12" => "duodecim"
    case "13" => "tredecim"
    case "14" => "quatuordecim"
    case "15" => "quindecim"
    case "16" => "sedecim"
    case "17" => "septendecim"
    case "18" => "octodecim"
    case "19" => "novemdecim"
    case "20" => "viginti"
    case "21" => "vigintiuno"
    case "22" => "vigintiduo"
    case "23" => "vigintitre"
    case "24" => "vigintiquatuor"
    case "25" => "vigintiquinque"
    case "26" => "vigintisex"
    case "27" => "vigintiseptem"
    case "28" => "vigintiocto"
    case "30" => "triginta"
    case "31" => "trigintauno"
    case "32" => "trigintaduo"
    case "38" => "trigintaocto"
    case "40" => "quadraginta"
    case _    => numeral
  }
}
