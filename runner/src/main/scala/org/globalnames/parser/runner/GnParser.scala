package org.globalnames
package parser
package runner

import java.io.{BufferedWriter, FileWriter}
import java.util.concurrent.atomic.AtomicInteger

import ScientificNameParser.{Result, instance => scientificNameParser}
import tcp.TcpServer
import web.controllers.WebServer
import resource._
import parser.BuildInfo

import scala.collection.parallel.ForkJoinTaskSupport
import scala.collection.parallel.immutable.ParVector
import scala.concurrent.forkjoin.ForkJoinPool
import scala.io.{Source, StdIn}
import scalaz.Scalaz._
import scalaz._

object GnParser {
  sealed trait Mode
  case object InputFileParsing extends Mode
  case object TcpServerMode extends Mode
  case object WebServerMode extends Mode
  case object NameParsing extends Mode

  case class Config(mode: Option[Mode] = Some(InputFileParsing),
                    inputFile: Option[String] = None,
                    outputFile: Option[String] = None,
                    host: String = "0.0.0.0",
                    port: Int = 4334,
                    name: String = "",
                    private val simpleFormat: Boolean = false,
                    private val threadsNumber: Option[Int] = None) {
    val parallelism: Int = threadsNumber.getOrElse(ForkJoinPool.getCommonPoolParallelism)
    def renderResult(result: Result): String =
      simpleFormat ? result.delimitedString() | result.renderCompactJson
  }

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("gnparser") {
      head("gnparser", BuildInfo.version)
      note("NOTE: if no command is provided then `file` is executed by default\n")
      help("help").text("prints this usage text")
      opt[Unit]('s', "simple").text("return simple CSV format instead of JSON")
        .optional.action { (x, c) => c.copy(simpleFormat = true) }
      cmd("name").action { (_, c) => c.copy(mode = NameParsing.some) }
                 .text("parse single scientific name").children(
        arg[String]("<scientific_name>").required.action { (x, c) => c.copy(name = x) }
      )
      cmd("file").action { (_, c) => c.copy(mode = InputFileParsing.some) }
                 .text("parse scientific names from input file").children(
        opt[String]('i', "input").text("if not present then input from <stdin>")
          .optional.valueName("<path_to_input_file>")
          .action { (x, c) => c.copy(inputFile = x.some) },
        opt[String]('o', "output").optional.text("if not present then output to <stdout>")
          .valueName("<path_to_output_file>")
          .action { (x, c) => c.copy(outputFile = x.some) },
        opt[Int]('t', "threads").valueName("<threads_number>")
          .action { (x, c) => c.copy(threadsNumber = x.some)}
      )
      cmd("socket").action { (_, c) => c.copy(mode = TcpServerMode.some) }
                   .text("run socket server for parsing").children(
        opt[Int]('p', "port").valueName("<port>")
                             .action { (x, c) => c.copy(port = x)},
        opt[String]('h', "host").valueName("<host>")
                                .action { (x, c) => c.copy(host = x) }
      )
      cmd("web").action { (_, c) => c.copy(mode = WebServerMode.some) }
                .text("run web server for parsing").children(
        opt[Int]('p', "port").valueName("<port>")
                             .action { (x, c) => c.copy(port = x) },
        opt[String]('h', "host").valueName("<host>")
                                .action { (x, c) => c.copy(host = x) }
      )
    }

    parser.parse(args, Config()) match {
      case Some(cfg) => cfg.mode.get match {
        case InputFileParsing => startFileParse(cfg)
        case TcpServerMode => TcpServer.run(cfg)
        case WebServerMode => WebServer.run(cfg)
        case NameParsing =>
          val result = scientificNameParser.fromString(cfg.name)
          println(cfg.renderResult(result))
      }
      case None =>
        Console.err.println("Invalid configuration of parameters. Check --help")
    }
  }

  def startFileParse(config: Config): Unit = {
    val inputIteratorEither = config.inputFile match {
      case None =>
        println("Enter scientific names line by line")
        val iterator = Iterator.continually(StdIn.readLine())
        iterator.takeWhile { str => str != null && str.trim.nonEmpty }.right
      case Some(fp) =>
        \/.fromEither(managed(Source.fromFile(fp)).map { _.getLines }.either.either)
    }

    val namesParsed = inputIteratorEither match {
      case -\/(errors) =>
        Console.err.println(errors.map { _.getMessage }.mkString("\n"))
        ParVector.empty[String]
      case \/-(res) =>
        val namesInputPar = res.toVector.par
        namesInputPar.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(config.parallelism))
        val parsedNamesCount = new AtomicInteger()

        println(s"Running with parallelism: ${config.parallelism}")
        for (name <- namesInputPar) yield {
          val currentParsedCount = parsedNamesCount.incrementAndGet()
          if (currentParsedCount % 10000 == 0) {
            println(s"Parsed $currentParsedCount of ${namesInputPar.size} lines")
          }
          val result = scientificNameParser.fromString(name.trim)
          config.renderResult(result)
        }
    }

    config.outputFile match {
      case Some(fp) =>
        for { writer <- managed(new BufferedWriter(new FileWriter(fp))) } {
          namesParsed.seq.foreach { name => writer.write(name + System.lineSeparator) }
        }
      case None => namesParsed.seq.foreach { name => println(name) }
    }
  }
}
