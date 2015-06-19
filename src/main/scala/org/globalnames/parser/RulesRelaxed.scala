package org.globalnames.parser

import org.parboiled2._
import CharPredicate.{Digit, Printable}

trait RulesRelaxed extends RulesClean {
  val az = "abcdefghijklmnopqrstuvwxyz-æœàâåãäáçčëéèíìïňññóòôøõöúùüŕřŗššşž"

  override def lowerChar = rule {
    CharPredicate(az)
  }
}
