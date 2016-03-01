package org.globalnames.parser

case class Warning(level: Int, message: String, astNodeId: Int)
  extends Ordered[Warning] {

  override def compare(that: Warning): Int =
    if (level != that.level) {
      -level.compareTo(that.level)
    } else {
      message.compareTo(that.message)
    }
}
