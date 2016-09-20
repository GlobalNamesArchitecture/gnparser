package org.globalnames

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._
import org.globalnames.parser.ScientificNameParser.{Result, instance => snp}

import scala.io.Source
import scala.util.Random

@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class ScientificNameParserBenchmark {
  import ScientificNameParserBenchmark.allNames

  @Param(Array("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"))
  var seed: Int = _

  var name: String = _

  @Setup
  def setup(): Unit = {
    val rand = new Random(seed)
    name = allNames(rand.nextInt(allNames.size)).verbatim
    println(name)
  }

  @Benchmark
  def fromString: Result= {
    snp.fromString(name)
  }
}

object ScientificNameParserBenchmark {
  case class ExpectedName(verbatim: String, json: String, simple: String)

  def expectedNames(filePath: String): Vector[ExpectedName] = {
    Source.fromFile(filePath, "UTF-8")
      .getLines
      .takeWhile { _.trim != "__END__" }
      .withFilter { line => !(line.isEmpty || ("#\r\n\f\t" contains line.charAt(0))) }
      .sliding(3, 3)
      .map { ls => ExpectedName(ls(0), ls(1), ls(2)) }
      .toVector
  }

  val allNames = expectedNames("../parser/src/test/resources/test_data.txt").drop(200)
}
