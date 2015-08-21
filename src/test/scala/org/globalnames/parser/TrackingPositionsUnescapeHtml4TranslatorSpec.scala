package org.globalnames.parser

import org.specs2.mutable.Specification

class TrackingPositionsUnescapeHtml4TranslatorSpec extends Specification {
  def positions(input: String): List[Int] = {
    val t = new TrackingPositionsUnescapeHtml4Translator()
    val translated = t.translate(input)
    List.range(0, translated.length + 1).map(t.at)
  }

  "Unescape translator should correctly handle" >> {
    "t" in {
      positions("t") === List(0, 1)
    }

    "Æ" in {
      positions("&AElig;") === List(0, 7)
    }

    "Tt" in {
      positions("Tt") === List(0, 1, 2)
    }

    "Æt" in {
      positions("&AElig;t") === List(0, 7, 8)
    }

    "Tð" in {
      positions("T&eth;") === List(0, 1, 6)
    }

    "Æð" in {
      positions("&AElig;&eth;") === List(0, 7, 12)
    }

    "Ætð" in {
      positions("&AElig;t&eth;") === List(0, 7, 8, 13)
    }
  }
}
