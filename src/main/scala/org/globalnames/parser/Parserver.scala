package org.globalnames.parser

object Parserver {
  def main(args: Array[String]) {
    val doc = """
    # Comment
    Betula
    Betula alba
    """

    val parsed = doc.lines map { line => new ParserClean(line).line.run() }

    parsed foreach println
  }
}
