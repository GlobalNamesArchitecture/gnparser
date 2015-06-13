package org.globalnames

import org.specs2.Specification
import scala.util._

/**
 * Created by dimus on 6/13/15.
 */
class ParserCleanAddSpec extends Specification{
  def is = s2"""Add two numbers
    |
    |1+1 = 2                      $e1
    |5-2 = 3                      $e2
    |5*2 = 10                     $e3
  """.stripMargin

  def e1 = new ParserClean("1+1").InputLine.run() must_== Success(2)
  def e2 = new ParserClean("5-2").InputLine.run() must_== Success(3)
  def e3 = new ParserClean("5*2").InputLine.run() must_== Success(10)
}
