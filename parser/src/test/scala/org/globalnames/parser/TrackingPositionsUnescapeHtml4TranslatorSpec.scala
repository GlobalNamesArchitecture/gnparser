package org.globalnames.parser

import org.specs2.mutable.Specification

class TrackingPositionsUnescapeHtml4TranslatorSpec extends Specification {
  def positions(input: String): List[Int] = {
    val t = new TrackingPositionsUnescapeHtml4Translator()
    val translated = t.translate(input)
    val positions = for (idx <- 0 until translated.length) yield t.at(idx)
    positions.toList
  }

  "Unescape translator should correctly handle" >> {
    "t" in {
      positions("t") === List(0)
    }

    "Æ" in {
      positions("&AElig;") === List(0)
    }

    "Tt" in {
      positions("Tt") === List(0, 1)
    }

    "Æt" in {
      positions("&AElig;t") === List(0, 7)
    }

    "Tð" in {
      positions("T&eth;") === List(0, 1)
    }

    "Æð" in {
      positions("&AElig;&eth;") === List(0, 7)
    }

    "Ætð" in {
      positions("&AElig;t&eth;") === List(0, 7, 8)
    }

    "<i>p</i>" in {
      positions("<i>p</i>") === List(3)
    }

    "&amp;p&nbsp;a</i>l<i>" in {
      positions("&amp;p&nbsp;a</i>l<i>") === List(0, 5, 6, 12, 17)
    }
  }
}
