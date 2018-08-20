package org.globalnames.parser

import ast.AstNode

case class WarningInfo(level: Int, message: String)

case class Warning(info: WarningInfo, node: AstNode) extends Ordered[Warning] {

  val level: Int = info.level
  val message: String = info.message

  override def compare(that: Warning): Int =
    if (level != that.level) {
      -level.compareTo(that.level)
    } else {
      message.compareTo(that.message)
    }
}

object Warning {
  def apply(level: Int, message: String, node: AstNode): Warning =
    Warning(WarningInfo(level, message), node)
}
