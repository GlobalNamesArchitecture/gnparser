package org.globalnames.parser

import org.parboiled2._
import scala.annotation.switch
import scala.util.{ Try, Success, Failure }

object ParseUtil {
  trait Line
  trait Word


  case class Comment() extends Line
  case class Blank() extends Line
  case class Name(verbatim: String) extends Line
  case class LatinWord(word: String) extends Word
  case class CapLatinWord(word: String) extends Word
}

class ParserClean(val input: ParserInput) extends Parser {
  import CharPredicate.{Printable}
  import ParseUtil._

  def line: Rule1[Line] = rule { noName | name }

  private def noName: Rule1[Line] = rule { blank | comment }

  private def name: Rule1[Line] = rule {
    softSpace ~ ( binomial | uninomial ) ~
      softSpace ~ EOI ~> (x => Name(x))
  }

  private def blank: Rule1[Line] = rule {
    softSpace ~ EOI ~ push(Blank())
  }

  private def comment: Rule1[Line] = rule {
    softSpace ~ '#' ~ zeroOrMore(Printable) ~
      EOI ~ push(Comment())
  }

  private def binomial: Rule1[String] = rule {
    capWord ~ space ~ word ~> ((w1: String, w2: String) =>
    s"$w1 $w2".toString)
  }

  private def uninomial: Rule1[String] = rule {
    capWord
  }
//
//  private def authorWord: Rule1[String] = rule {
//    capture(CharPredicate.UpperAlpha ~
//      CharPredicate.LowerAlpha ~ '.'.?)
//  }

  private def capWord: Rule1[String] = rule {
    capture(upperChar ~ oneOrMore(lowerChar))
  }

  private def word: Rule1[String] = rule {
    capture(lowerChar ~ oneOrMore(lowerChar))
  }

  private def upperChar = rule {
    CharPredicate("ABCDEFGHIJKLMNOPQRSTUVWXYZËÆŒ")
  }

  private def lowerChar = rule {
    CharPredicate("abcdefghijklmnopqrstuvwxyzëæœ")
  }

  private def softSpace = rule { zeroOrMore(spaceChars) }

  private def space = rule { oneOrMore(spaceChars) }

  private def spaceChars = rule { CharPredicate(" \t\r\n\f") }
}
