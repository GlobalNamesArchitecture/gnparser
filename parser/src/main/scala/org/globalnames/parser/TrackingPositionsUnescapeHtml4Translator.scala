package org.globalnames.parser

import java.io.Writer

import org.apache.commons.text.translate._

import scala.collection.mutable.ArrayBuffer

class TrackingPositionsUnescapeHtml4Translator extends AggregateTranslator {
  import TrackingPositionsUnescapeHtml4Translator.translators

  private final val positions = ArrayBuffer.fill(1)(0)
  final var identity: Boolean = true

  override def translate(input: CharSequence, index: Int, out: Writer): Int = {
    val consumed = translators.translate(input, index, out)
    identity &&= consumed == 0
    positions += positions.last + (if (consumed == 0) 1 else consumed)
    consumed
  }

  def at(pos: Int): Int = if (identity) pos else positions(pos)
}

object TrackingPositionsUnescapeHtml4Translator {
  private[TrackingPositionsUnescapeHtml4Translator] final val translators = {
    val translator1 = {
      val entityArrays = new java.util.HashMap[CharSequence, CharSequence]
      entityArrays.putAll(EntityArrays.BASIC_UNESCAPE)
      entityArrays.putAll(EntityArrays.ISO8859_1_UNESCAPE)
      entityArrays.putAll(EntityArrays.HTML40_EXTENDED_UNESCAPE)
      new LookupTranslator(entityArrays)
    }
    val translator2 = new NumericEntityUnescaper
    new AggregateTranslator(translator1, translator2)
  }
}
