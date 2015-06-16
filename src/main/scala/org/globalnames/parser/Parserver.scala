package org.globalnames.parser

import scala.util.{Try, Success, Failure}
import org.globalnames.parser.ParseUtil._
import org.parboiled2._

object Parserver {
  def main(args: Array[String]) {
    val doc = """
    # Comment
    Betula
    Betula alba
    Betula alba       Linnaeus
    Betula alba Linn.
    Betula alba L. 1758
    """

    val parsed = doc.lines foreach { line => pparse(line) }
  }

  private def pparse(name: String) {
    val pc = new ParserClean(name)
    val parsed = pc.line.run()
    parsed match {
      case Success(x: Name) => println(x.verbatim)
      case Success(_) =>
      case Failure(e: ParseError) => println(pc.formatError(e))
      case Failure(e) => println(e)
    }
  }

}
