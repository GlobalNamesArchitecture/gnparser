package org.globalnames.parser

case class Warning(level: Int, message: String, astNodeId: Int)
  extends Ordered[Warning] {

  import scala.math.Ordered.orderingToOrdered

  override def compare(that: Warning): Int =
    (-level, message).compare((-that.level, that.message))
}
