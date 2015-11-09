package org.globalnames

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._
import org.globalnames.parser.ScientificNameParser.{instance ⇒ snp, Result}

@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class ScientificNameParserBenchmark {
  @Param(Array("Helvella macropus",
               "Helvella leucophaea Pers., 1801",
               "Helvella lycoperdoides Scop. 1772",
               "Helvella lilacina Wulfen, 1786",
               "Helvella leucopus var. populina I. Arroyo & Calonge 2000"))
  var name: String = _

  @Benchmark
  def fromString: Result = {
    snp.fromString(name)
  }
}
