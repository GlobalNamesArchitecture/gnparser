package org.globalnames.parser

import org.parboiled2._
import CharPredicate.{Digit, Printable}

trait RulesRelaxed extends RulesClean {
  override def sciName: Rule1[SciName] = rule {
    softSpace ~ sciName1 ~ softSpace ~ garbage.? ~ EOI ~> ((x: Node) =>
      SciName(
        verbatim = input.sliceString(0, input.length),
        normalized =  Some(x.normalized),
        canonical = Some(x.canonical),
        isParsed = true,
        parserRun = 2
      )
    )
  }

  def garbage = rule {
    oneOrMore(CharPredicate(Printable))
  }

  val az = "abcdefghijklmnopqrstuvwxyz-æœàâåãäáçčëéèíìïňññóòôøõöúùüŕřŗššşž"

  override def lowerChar = rule {
    CharPredicate(az)
  }
}
