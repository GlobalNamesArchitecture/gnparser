package org.globalnames.parser.examples

import org.apache.spark.{SparkConf, SparkContext}
import org.globalnames.parser.ScientificNameParser.{instance => snp}

object ParserSpark {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("abc")
    val sc   = new SparkContext(conf)

    val names = List("Homo Sapiens Linnaeus 1758",
                     "Salinator solida Martens 1878",
                     "Kanisamys indicus Wood 1937")
    val canonicals = sc.parallelize(names).map { name =>
      val canonical = snp.fromString(name).summary().canonicalName
      canonical.map { _.value }.getOrElse("")
    }
    println(canonicals.collect().mkString("\n"))
  }
}
