package org.globalnames

import java.io.{BufferedWriter, FileWriter}

import parser.ScientificNameParser.{instance ⇒ scientificNameParser}
import parser.runner.web.controllers.WebServer
import parser.runner.tcp.TcpServer
import runner.BuildInfo

import scala.collection.parallel.ForkJoinTaskSupport
import scala.concurrent.forkjoin.ForkJoinPool
import scala.io.Source
import scala.util.{Failure, Success, Try}

import scalaz._
import Scalaz._

object GnParser {
  sealed trait Mode
  case object InputFileParsing extends Mode
  case object TcpServerMode extends Mode
  case object WebServerMode extends Mode
  case object NameParsing extends Mode

  case class Config(mode: Option[Mode] = Some(WebServerMode),
                    inputFile: Option[String] = None,
                    outputFile: Option[String] = None,
                    host: String = "0.0.0.0",
                    port: Int = 4334,
                    name: String = "",
                    simpleFormat: Boolean = false,
                    threadsNumber: Option[Int] = None)

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("gnparse") {
      head("gnparse", BuildInfo.version)
      help("help").text("prints this usage text")
      opt[Unit]('s', "simple").text("simple CSV format")
        .optional.action { (x, c) => c.copy(simpleFormat = true) }
      cmd("file").action { (_, c) => c.copy(mode = InputFileParsing.some) }
                 .text("file command").children(
        opt[String]('i', "input").required.valueName("<path_to_input_file>")
          .action { (x, c) => c.copy(inputFile = x.some) },
        opt[String]('o', "output").required.valueName("<path_to_output_file>")
          .action { (x, c) => c.copy(outputFile = x.some) },
        opt[Int]('t', "threads").valueName("<threads_number>")
          .action { (x, c) => c.copy(threadsNumber = x.some)}
      )
      cmd("socket").action { (_, c) => c.copy(mode = TcpServerMode.some) }
                   .text("socket server command").children(
        opt[Int]('p', "port").valueName("<port>")
                             .action { (x, c) => c.copy(port = x)},
        opt[String]('h', "host").valueName("<host>")
                                .action { (x, c) => c.copy(host = x) }
      )
      cmd("web").action { (_, c) => c.copy(mode = WebServerMode.some) }
                .text("web-api command").children(
        opt[Int]('p', "port").valueName("<port>")
                             .action { (x, c) => c.copy(port = x) },
        opt[String]('h', "host").valueName("<host>")
                                .action { (x, c) => c.copy(host = x) }
      )
      cmd("name").action { (_, c) => c.copy(mode = NameParsing.some) }
                 .text("name command").children(
        arg[String]("scientific name").required
                                      .action { (x, c) => c.copy(name = x) }
      )
    }

    parser.parse(args, Config()) match {
      case Some(cfg) if cfg.mode.get == InputFileParsing =>
        startFileParse(cfg.inputFile.get, cfg.outputFile.get,
                       cfg.threadsNumber, cfg.simpleFormat)
      case Some(cfg) if cfg.mode.get == TcpServerMode =>
        TcpServer.run(cfg.host, cfg.port, cfg.simpleFormat)
      case Some(cfg) if cfg.mode.get == WebServerMode =>
        WebServer.run(cfg.host, cfg.port)
      case Some(cfg) if cfg.mode.get == NameParsing =>
        val result = scientificNameParser.fromString(cfg.name)
        println {
          if (cfg.simpleFormat) result.delimitedString()
          else result.renderCompactJson
        }
      case None =>
        Console.err.println("Invalid configuration of parameters. Check --help")
    }
  }

  def startFileParse(inputFilePath: String,
                     outputFilePath: String,
                     threadsNumber: Option[Int],
                     simpleFormat: Boolean) =
    Try(Source.fromFile(inputFilePath)) match {
      case Failure(e) => Console.err.println(s"No such file: $inputFilePath")
      case Success(f) =>
        val parallelism =
          threadsNumber.getOrElse(ForkJoinPool.getCommonPoolParallelism)
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
          val result = scientificNameParser.fromString(name.trim)
          if (simpleFormat) result.delimitedString()
          else result.renderCompactJson
        }
        val writer = new BufferedWriter(new FileWriter(outputFilePath))
        namesParsed.seq.foreach { name ⇒
          writer.write(name + System.lineSeparator)
        }
        writer.close()
    }
}
