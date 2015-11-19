package org.globalnames

import java.io.{BufferedWriter, FileWriter}
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.{CountDownLatch, Executors}

import org.globalnames.parser.ScientificNameParser.{instance ⇒ scientificNameParser}

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
          val batchSize = Option(sys.props("batch")).map { _.toInt }
            .getOrElse(100)
          println(s"running with: parallelism=$parallelism batch=$batchSize")
          val namesInput = f.getLines().toArray
          val pool = Executors.newFixedThreadPool(parallelism)
          val parsedNamesCount = new java.util.concurrent.atomic.AtomicInteger()
          val workersActive = new CountDownLatch(parallelism)
          val start = System.nanoTime()
          for (_ <- 1 to parallelism) {
            pool.submit(new Runnable {
              override def run(): Unit = {
                var parsing = true
                while (parsing) {
                  val currentParsedNamesCount = parsedNamesCount.getAndIncrement()
                  parsing = currentParsedNamesCount < namesInput.length / batchSize
                  if (parsing) {
                    if (currentParsedNamesCount % 100 == 0) {
                      println(
                        s"""Parsed ${currentParsedNamesCount * batchSize} """ +
                        s"""of ${namesInput.length} lines""")
                    }
                    val beginIdx = currentParsedNamesCount * batchSize
                    val endIdx = math.min((currentParsedNamesCount + 1) * batchSize,
                      namesInput.length)
                    for (i ← beginIdx until endIdx) {
                      val name = namesInput(i).trim
                      val result = scientificNameParser.fromString(name)
                      namesInput(i) = result.renderCompactJson
                    }
                  }
                }
                workersActive.countDown()
              }
            })
          }
          workersActive.await()
          val end = System.nanoTime()
          println("Processed for " +
            NumberFormat.getNumberInstance(Locale.US).format((end - start)/1000000) +
            "ms")
          pool.shutdown()
          val writer = new BufferedWriter(new FileWriter(outputFilePath))
          namesInput.seq.foreach { name ⇒
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
