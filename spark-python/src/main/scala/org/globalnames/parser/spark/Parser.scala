package org.globalnames.parser.spark

import org.apache.spark.api.java.JavaRDD
import org.apache.spark.api.java.function.Function
import org.globalnames.parser.ScientificNameParser.{instance => snp}

class Parser {
  def parse(names: JavaRDD[String], compact: Boolean, showCanonicalUuid: Boolean): JavaRDD[String] =
    names.map(new Function[String, String] {
      override def call(name: String): String =
        snp.fromString(name).renderJsonString(compact, showCanonicalUuid)
    })
}
