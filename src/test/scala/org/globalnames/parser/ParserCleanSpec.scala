package org.globalnames.parser

import org.specs2.mutable.Specification
import scala.util.{Success, Failure}

class ParserCleanSpec extends Specification {
  "ParserClean" should {
    "parse carramba" in parse("2") === 2
  }
  "no parse carramba" in parse("what the ?") === 0

  def parse(input: String): Int = {
    val pc = new ParserClean(input)
    val parsed = pc.InputLine.run()
    parsed match {
      case Success(res: Int) => res
      case Failure(err) => {
        println(err)
        0
      }
    }
  }
}
