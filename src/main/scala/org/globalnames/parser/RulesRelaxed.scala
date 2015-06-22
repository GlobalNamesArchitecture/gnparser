package org.globalnames.parser

import org.parboiled2._
import CharPredicate.{Digit, Printable}

trait RulesRelaxed extends RulesClean {
  override def sciName: Rule1[SciName] = rule {
    softSpace ~ sciName1 ~ softSpace ~ EOI ~> ((x: Node) =>
      SciName(
        verbatim = input.sliceString(0, input.length),
        normalized =  Some(x.normalized),
        canonical = Some(x.canonical),
        isParsed = true,
        parserRun = 2
      )
    )
  }

  override def sciName1: Rule1[Node] = rule {
    (nameAuthor | name) ~ garbage.?
  }

  def garbage = rule {
    space ~ oneOrMore(CharPredicate(Printable))
  }

  val az = "abcdefghijklmnopqrstuvwxyz-æœàâåãäáçčëéèíìïňññóòôøõöúùüŕřŗššşž"

  override def lowerChar = rule {
    CharPredicate(az)
  }
}
