package org.globalnames.parser

import java.io.Writer

import org.apache.commons.lang3.text.translate._

import scala.collection.mutable.ArrayBuffer

class TrackingPositionsUnescapeHtml4Translator extends AggregateTranslator {
  private final val translators =
    List(EntityArrays.BASIC_UNESCAPE,
         EntityArrays.ISO8859_1_UNESCAPE,
         EntityArrays.HTML40_EXTENDED_UNESCAPE()).map { strss =>
    new LookupTranslator(strss.asInstanceOf[Array[Array[CharSequence]]]: _*)
  } :+ new NumericEntityUnescaper

  private final val positions = ArrayBuffer.fill(1)(0)
  private final var identity = true

  override def translate(input: CharSequence, index: Int, out: Writer): Int = {
    val consumed = translators.foldLeft(0) { (cnsmed, cst) =>
      if (cnsmed == 0) cst.translate(input, index, out)
      else cnsmed
    }
    positions += positions.last + (if (consumed == 0) 1
                                   else { identity = false; consumed })
    consumed
  }

  def at(pos: Int): Int = if (identity) pos else positions(pos)
}
