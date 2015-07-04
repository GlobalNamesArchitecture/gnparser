package org.globalnames.parser

import scala.util.{Try, Success, Failure}
import org.parboiled2._

object Parserver {
  val usage = """
    Usage:
      - to run a server (default: --port 4334)
      parserver --server [--port 1234]

      - to parse names from a file (default output is output.json)
      parserver --input file_with_names.txt [--output output_file.json]

      - to parse one name:
      parserver "Betula alba"
  """

  def main(args: Array[String]) {
    if (args.length == 0) {
      println(usage)
      System.exit(0)
    }

    val argList = args.toList
    type OptionMap = Map[Symbol, Any]

    def nextOption(map: OptionMap, list: List[String]): OptionMap = {
      list match {
        case Nil => map
        case "--input" :: value :: tail =>
          nextOption(map ++ Map('input -> value.toString), tail)
        case "--output" :: value :: tail =>
          nextOption(map ++ Map('output -> value.toString), tail)
        case "--server" :: tail =>
          nextOption(map ++ Map('server -> true), tail)
        case "--port" :: value :: tail =>
          nextOption(map ++ Map('port -> value.toInt), tail)
        case string :: Nil =>
          nextOption(map ++ Map('name -> string), list.tail)
        case option :: tail => {
          println("Unknown option " + option)
          System.exit(1)
          map
        }
      }
    }
    val options = nextOption(Map(), argList)
    println(options)
  }
}
