package org.globalnames.parser

class ParserWarnings {
  import ParserWarnings.Warning

  var warnings = Vector.empty[Warning]

  def add(warning: Warning): Unit = warnings :+= warning
}

object ParserWarnings {
  case class Warning(level: Int, message: String, astNode: AstNode)
}
