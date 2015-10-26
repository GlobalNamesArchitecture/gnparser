package org.globalnames

import java.io.{File, PrintWriter}

import org.globalnames.parser.ScientificNameParser.{instance => scientificNameParser}

import scala.io.Source
import scala.util.{Failure, Success, Try}

object GnParser {
  def main(args: Array[String]) {
    if (args.length == 0) {
      Console.err.println("No args found. Type -h for help")
      System.exit(0)
    }

    val argList = args.toList
    type OptionMap = Map[Symbol, String]

    def nextOption(map: OptionMap, list: List[String]): OptionMap = {
      list match {
        case Nil => map
        case "-input" :: value :: tail =>
          nextOption(map ++ Map('input -> value), tail)
        case "-output" :: value :: tail =>
          nextOption(map ++ Map('output -> value), tail)
        case "-server" :: tail =>
          nextOption(map ++ Map('server -> "true"), tail)
        case "-port" :: value :: tail =>
          nextOption(map ++ Map('port -> value.toString), tail)
        case string :: Nil =>
          nextOption(map ++ Map('name -> string), list.tail)
        case option :: tail =>
          Console.err.println("Unknown option " + option)
          System.exit(1)
          map
      }
    }

    def startServerParse(port: Int) = {
      ParServer(port).run()
    }

    def startFileParse(input: String, output: String) = {
      val writer = new PrintWriter(new File(output))
      Try(Source.fromFile(input)) match {
        case Failure(e) => Console.err.println(s"No such file: $input")
        case Success(f) =>
          f.getLines().zipWithIndex.foreach {
            case (line, i) =>
              if ((i + 1) % 10000 == 0) println(s"Parsed ${i + 1} lines")
              val parsed = scientificNameParser.renderCompactJson(
                scientificNameParser.fromString(line.trim))
              writer.write(parsed + "\n")
          }
      }
      writer.close()
    }

    val options = nextOption(Map(), argList)

    options match {
      case o if o.contains('server) =>
        val port = if (o.contains('port)) o('port) else "4334"
        startServerParse(port.toInt)
      case o if o.contains('input) =>
        val input = o('input)
        val output = if (o.contains('output)) o('output) else "output.json"
        startFileParse(input, output)
      case o if o.contains('name) =>
        println(scientificNameParser.renderCompactJson(
          scientificNameParser.fromString(o('name))))
    }
  }
}
