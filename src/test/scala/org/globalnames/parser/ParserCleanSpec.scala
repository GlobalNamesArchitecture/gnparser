package org.globalnames.parser

import org.specs2.mutable.Specification
import scala.util.{Success, Failure}

class ParserCleanSpec extends Specification {
  "ParserClean" should {
    "parse 1+1 to 2" in
      parse("1+1") === 2
    "parse 2-1 to 1" in
      parse("2-1") === 1
  }

  def parse(input: String): Int = {
    val pc = new ParserClean(input).InputLine.run()
    pc match {
      case Success(res: Int) => res
      case Failure(_) => 0
    }
  }
}
