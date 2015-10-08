package org.globalnames.parser

class ParserWarnings {
  import ParserWarnings.Warning

  var warnings = Vector.empty[Warning]

  def worstLevel = {
    if (warnings.isEmpty) 1
    else warnings.sortBy { _.level }.last.level
  }

  def add(warning: Warning): Unit = warnings :+= warning
}

object ParserWarnings {
  case class Warning(level: Int, message: String, astNode: AstNode)
    extends Ordered[Warning] {

    import scala.math.Ordered.orderingToOrdered

    override def compare(that: Warning): Int =
      (-level, message).compare((-that.level, that.message))
  }
}
