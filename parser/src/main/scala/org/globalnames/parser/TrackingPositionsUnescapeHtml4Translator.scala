package org.globalnames.parser

import java.io.{StringWriter, Writer}

import org.apache.commons.text.translate._

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._

class TrackingPositionsUnescapeHtml4Translator extends AggregateTranslator {
  import TrackingPositionsUnescapeHtml4Translator.translators

  final var positions: ArrayBuffer[Int] = ArrayBuffer[Int]()
  final var identity: Boolean = true
  final var indexCurrent: Int = _

  def translate(input: String): String = {
    // NOTE: `override def translate(input: CharSequence, index: Int, out: Writer): Int`
    // has no friendly way to track the positioning. Consider tracking positions for
    // `<i>Homo</i> sapiens Linn. &amp; Markov`: `<i>` maps to empty string, and `&amp;` to `&`.
    // And the translator needs to track if the `Char` was actually added to the output to track it
    // Hence, we're redefining the `translate` method to keep track on the `out: Writer`

    val stringWriter = new StringWriter(input.length * 2) {
      override def write(ch: Int): Unit = {
        positions += indexCurrent
        super.write(ch)
      }

      override def write(str: String): Unit = {
        (0 until str.length).foreach { _ => positions += indexCurrent }
        super.write(str)
      }
    }

    this.translate(input, stringWriter)
    stringWriter.toString
  }

  override def translate(input: CharSequence, index: Int, out: Writer): Int = {
    indexCurrent = index
    val consumed = translators.translate(input, index, out)
    identity &&= consumed == 0
    consumed
  }

  def at(pos: Int): Int = if (identity) pos else positions(pos)
}

object TrackingPositionsUnescapeHtml4Translator {
  private val HTML_TAGS_REMOVE = Map("<i>" -> "", "</i>" -> "").asJava

  private[TrackingPositionsUnescapeHtml4Translator] final val translators = {
    val translator1 = {
      val entityArrays = new java.util.HashMap[CharSequence, CharSequence]
      entityArrays.putAll(EntityArrays.BASIC_UNESCAPE)
      entityArrays.putAll(EntityArrays.ISO8859_1_UNESCAPE)
      entityArrays.putAll(EntityArrays.HTML40_EXTENDED_UNESCAPE)
      entityArrays.putAll(HTML_TAGS_REMOVE)
      new LookupTranslator(entityArrays)
    }
    val translator2 = new NumericEntityUnescaper
    new AggregateTranslator(translator1, translator2)
  }
}
