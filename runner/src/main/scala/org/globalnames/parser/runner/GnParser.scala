package org.globalnames
package parser
package runner

import java.io.{BufferedWriter, FileWriter}
import java.util.concurrent.atomic.AtomicInteger

import ScientificNameParser.{instance => scientificNameParser}
import tcp.TcpServer
import web.controllers.WebServer
import resource._

import scala.collection.parallel.ForkJoinTaskSupport
import scala.collection.parallel.immutable.ParVector
import scala.concurrent.forkjoin.ForkJoinPool
import scala.io.{Source, StdIn}

import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.syntax.either._
import scalaz.{\/, -\/, \/-}

import org.json4s.JsonAST.JArray
import org.json4s.jackson.JsonMethods

object GnParser {
  sealed trait Mode
  object Mode {
    case object InputFileParsing extends Mode
    case object TcpServerMode extends Mode
    case object WebServerMode extends Mode
    case object NameParsing extends Mode
  }

  sealed trait Format
  object Format {
    case object Simple extends Format
    case object JsonPretty extends Format
    case object JsonCompact extends Format

    def parse(v: String): Format = v match {
      case "simple" => Format.Simple
      case "json-pretty" => Format.JsonPretty
      case "json-compact" => Format.JsonCompact
      case x => throw new IllegalArgumentException(s"Unexpected value of `format` flag: $x")
    }
  }

  case class Config(mode: Mode = Mode.InputFileParsing,
                    inputFile: Option[String] = None,
                    outputFile: Option[String] = None,
                    host: String = "0.0.0.0",
                    port: Int = 4334,
                    name: String = "",
                    private val format: Format = Format.JsonPretty,
                    private val threadsNumber: Option[Int] = None) {

    val parallelism: Int = threadsNumber.getOrElse(ForkJoinPool.getCommonPoolParallelism)

    def renderResult(result: RenderableResult): String = format match {
      case Format.Simple => result.renderDelimitedString()
      case Format.JsonCompact => result.renderJson(compact = true)
      case Format.JsonPretty => result.renderJson(compact = false)
    }

    def resultsToJson(results: Vector[RenderableResult]): String = format match {
      case Format.Simple =>
        val resultsStrings = for (r <- results) yield r.renderDelimitedString()
        resultsStrings.mkString("\n")
      case f =>
        val resultsJsonArr = for (r <- results.toList) yield r.json
        val resultsJson = JArray(resultsJsonArr)
        val resultString = f match {
          case Format.JsonCompact => JsonMethods.compact(resultsJson)
          case _ => JsonMethods.pretty(resultsJson)
        }
        resultString
    }

  }

  private[runner] val gnParserVersion = BuildInfo.version
  private[runner] val welcomeMessage = "Enter scientific names line by line"
  private[runner] val fileCommandName = "file"
  private[runner] def actualCommandLineArgs(args: Array[String]): String = {
    s"Actual command line args: ${args.mkString(" ")}"
  }

  private val parser = new scopt.OptionParser[Config]("gnparser") {
    head("gnparser", gnParserVersion)
    head("NOTE: if no command is provided then `file` is executed by default")
    help("help").text("prints this usage text")

    opt[String]('f', "format")
      .text("format representation: simple, json-compact or json-pretty").optional
      .action { (x, c) => c.copy(format = Format.parse(x)) }

    cmd("name").action { (_, c) => c.copy(mode = Mode.NameParsing) }
               .text("parse single scientific name").children(
      arg[String]("<scientific_name>").required.action { (x, c) => c.copy(name = x) }
    )

    cmd(fileCommandName).action { (_, c) => c.copy(mode = Mode.InputFileParsing) }
               .text("parse scientific names from input file (default)").children(
      opt[String]('i', "input").text("if not present then input from <stdin>")
        .optional
        .valueName("<path_to_input_file>")
        .action { (x, c) => c.copy(inputFile = x.some) },
      opt[String]('o', "output").text("if not present then output to <stdout>")
        .optional
        .valueName("<path_to_output_file>")
        .action { (x, c) => c.copy(outputFile = x.some) },
      opt[Int]('t', "threads")
        .optional
        .valueName("<threads_number>")
        .action { (x, c) => c.copy(threadsNumber = x.some)}
    )

    cmd("socket").action { (_, c) => c.copy(mode = Mode.TcpServerMode) }
               .text("run socket server for parsing").children(
      opt[Int]('p', "port").valueName("<port>").action { (x, c) => c.copy(port = x)},
      opt[String]('h', "host").valueName("<host>").action { (x, c) => c.copy(host = x) }
    )

    cmd("web").action { (_, c) => c.copy(mode = Mode.WebServerMode) }
               .text("run web server for parsing").children(
      opt[Int]('p', "port").valueName("<port>").action { (x, c) => c.copy(port = x) },
      opt[String]('h', "host").valueName("<host>").action { (x, c) => c.copy(host = x) }
    )

  }

  protected[runner] def parse(args: Array[String]): Option[Config] = {
    val argsWithDefaultCommand =
      (args.isEmpty || args(0).startsWith("-")) ? (fileCommandName +: args) | args
    Console.err.println(actualCommandLineArgs(argsWithDefaultCommand))
    val parsedArgs = parser.parse(argsWithDefaultCommand, Config())
    parsedArgs
  }

  def main(args: Array[String]): Unit = {
    for {cfg <- parse(args); mode = cfg.mode} {
      mode match {
        case Mode.InputFileParsing => startFileParse(cfg)
        case Mode.TcpServerMode => TcpServer.run(cfg)
        case Mode.WebServerMode => WebServer.run(cfg)
        case Mode.NameParsing =>
          val result = scientificNameParser.fromString(cfg.name)
          println(cfg.renderResult(result))
      }
    }
  }

  def startFileParse(config: Config): Unit = {
    val inputIteratorEither = config.inputFile match {
      case None =>
        Console.err.println(welcomeMessage)
        val iterator = Iterator.continually(StdIn.readLine())
        iterator.takeWhile { str => str != null && str.trim.nonEmpty }.right
      case Some(fp) =>
        val sourceFile = Source.fromFile(fp)
        val inputLinesManaged = managed(sourceFile).map { _.getLines.toVector }
        \/.fromEither(inputLinesManaged.either.either)
    }

    val namesParsed = inputIteratorEither match {
      case -\/(errors) =>
        Console.err.println(errors.map { _.getMessage }.mkString("\n"))
        ParVector.empty
      case \/-(res) =>
        val namesInputPar = res.toVector.par
        namesInputPar.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(config.parallelism))
        val parsedNamesCount = new AtomicInteger()

        Console.err.println(s"Running with parallelism: ${config.parallelism}")
        val start = System.nanoTime()
        for (name <- namesInputPar) yield {
          val currentParsedCount = parsedNamesCount.incrementAndGet()
          if (currentParsedCount % 10000 == 0) {
            val elapsed = (System.nanoTime() - start) * 1e-6
            val msg =
              f"Parsed $currentParsedCount of ${namesInputPar.size} lines, elapsed $elapsed%.2fms"
            Console.err.println(msg)
          }
          val result = scientificNameParser.fromString(name.trim)
          result
        }
    }

    val resultsJsonStr = config.resultsToJson(namesParsed.seq)

    config.outputFile match {
      case Some(fp) =>
        for { writer <- managed(new BufferedWriter(new FileWriter(fp))) } {
          writer.write(resultsJsonStr)
        }
      case None => println(resultsJsonStr)
    }
  }
}
