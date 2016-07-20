package org.globalnames.parser

import java.io.Writer

import org.apache.commons.lang3.text.translate._

import scala.collection.mutable.ArrayBuffer

class TrackingPositionsUnescapeHtml4Translator extends AggregateTranslator {
  import TrackingPositionsUnescapeHtml4Translator.translators

  private final val positions = ArrayBuffer.fill(1)(0)
  final var identity: Boolean = true

  override def translate(input: CharSequence, index: Int, out: Writer): Int = {
    val consumed = translators.foldLeft(0) { (cnsmed, cst) =>
      if (cnsmed == 0) cst.translate(input, index, out)
      else cnsmed
    }
    identity &&= consumed == 0
    positions += positions.last + (if (consumed == 0) 1 else consumed)
    consumed
  }

  def at(pos: Int): Int = if (identity) pos else positions(pos)
}

object TrackingPositionsUnescapeHtml4Translator {
  private[TrackingPositionsUnescapeHtml4Translator] final val translators = {
    val translator1 = new LookupTranslator(
      (EntityArrays.BASIC_UNESCAPE ++
        EntityArrays.ISO8859_1_UNESCAPE ++
        EntityArrays.HTML40_EXTENDED_UNESCAPE).asInstanceOf[Array[Array[CharSequence]]]: _*)
    val translator2 = new NumericEntityUnescaper
    List(translator1, translator2)
  }
}
