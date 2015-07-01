package org.globalnames.parser

import scala.util.{Try, Success, Failure}
import org.parboiled2._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._


object Parserver {
  def main(args: Array[String]) {
    val conf = new SparkConf().setMaster("local").setAppName("Parserver")
    val sc = new SparkContext(conf)
    println(args(0))
  }

}
