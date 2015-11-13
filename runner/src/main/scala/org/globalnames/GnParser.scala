package org.globalnames

import java.io.{BufferedWriter, FileWriter}

import org.globalnames.parser.ScientificNameParser.{instance ⇒ scientificNameParser}

import scala.collection.parallel.ForkJoinTaskSupport
import scala.concurrent.forkjoin.ForkJoinPool
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

    def startFileParse(inputFilePath: String, outputFilePath: String) =
      Try(Source.fromFile(inputFilePath)) match {
        case Failure(e) => Console.err.println(s"No such file: $inputFilePath")
        case Success(f) =>
          val parallelism = Option(sys.props("parallelism")).map { _.toInt }
            .getOrElse(ForkJoinPool.getCommonPoolParallelism)
          println(s"running with parallelism: $parallelism")
          val parsedNamesCount = new java.util.concurrent.atomic.AtomicInteger()
          val namesInput = f.getLines().toVector.par
          namesInput.tasksupport =
            new ForkJoinTaskSupport(new ForkJoinPool(parallelism))
          val namesParsed = namesInput.map { name ⇒
            val currentParsedCount = parsedNamesCount.incrementAndGet()
            if (currentParsedCount % 10000 == 0) {
              println(s"Parsed $currentParsedCount of ${namesInput.size} lines")
            }
            scientificNameParser.fromString(name.trim).renderCompactJson
          }
          val writer = new BufferedWriter(new FileWriter(outputFilePath))
          namesParsed.seq.foreach { name ⇒
            writer.write(name + System.lineSeparator)
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
        println(scientificNameParser.fromString(o('name)).renderCompactJson)
    }
  }
}
