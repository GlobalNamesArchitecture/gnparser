package org.globalnames
package parser
package runner

import java.io.{Console => JavaConsole, _}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import GnParser.{Config, InputFileParsing}
import org.specs2.matcher.{MatcherMacros, StringMatchers}
import org.specs2.mutable.Specification

import scala.io.Source

class GnParserSpec extends Specification with StringMatchers with MatcherMacros {
  "GnParserSpec" >> {
    "should parse from file by default" >> {
      GnParser.parse(Array()).get must matchA[Config].mode(Some(InputFileParsing))
    }

    "should have correct version" >> {
      GnParser.gnParserVersion must_== org.globalnames.parser.BuildInfo.version
    }

    def gnparse(args: String): Unit = {
      GnParser.main(args.split("\\s+"))
    }

    def withInOut(input: String, thunk: () => Unit): Array[String] = {
      val streamIn = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))
      Console.withIn(streamIn) { withOut(thunk) }
    }

    def withOut(thunk: () => Unit): Array[String] = {
      val streamOut = new ByteArrayOutputStream()
      Console.withOut(streamOut) {
        thunk()
        streamOut.toString(StandardCharsets.UTF_8.displayName).split("\n")
      }
    }

    "it handles `file` option" >> {
      val (name1, name2) = ("Homo sapiens", "Salinator solida")
      val Seq(name1uuid, name2uuid) =
        Seq(name1, name2).map { n => UuidGenerator.generate(n).toString }
      val namesInputFilePath: Path = {
        def namesFileContent: String = s"""$name1
                                          |$name2""".stripMargin
        val file = Files.createTempFile("names", ".txt")
        Files.write(file, namesFileContent.getBytes(StandardCharsets.UTF_8))
      }

      "should have correct preamble" >> {
        val lines = withInOut("", () => gnparse("file -s"))
        lines should have size 2
        lines(0) should_== GnParser.welcomeMessage
        lines(1) must startWith("Running with parallelism")
      }

      "should parse single name from <stdin>" >> {
        val lines = withInOut(name1, () => gnparse("file -s"))
        lines should have size 3
        lines(2) must startWith(name1uuid)
      }

      "should parse from <stdin> to <stdout> when no file is provided" >> {
        val input = s"""$name1
                       |$name2""".stripMargin
        val lines = withInOut(input, () => gnparse("file -s"))
        lines should have size 4
        lines.drop(2).toSeq must contain(exactly(startWith(name1uuid), startWith(name2uuid)))
      }

      "should parse from input file to <stdout>" >> {
        val lines = withOut(() => gnparse(s"file -s -i $namesInputFilePath"))
        lines should have size 3
        lines.drop(1).toSeq must contain(exactly(startWith(name1uuid), startWith(name2uuid)))
      }

      "should parse from <stdin> to output file" >> {
        val input = s"""$name1
                       |$name2""".stripMargin
        val nameOutputFilePath = Files.createTempFile("names_output1", ".txt")
        val lines = withInOut(input, () => gnparse(s"file -s -o $nameOutputFilePath"))
        lines should have size 2
        val linesOut = Source.fromFile(nameOutputFilePath.toFile).getLines.toVector
        linesOut should have size 2
        linesOut must contain(exactly(startWith(name1uuid), startWith(name2uuid)))
      }

      "should parse from input file to output file" >> {
        val nameOutputFilePath = Files.createTempFile("names_output2", ".txt")
        val lines = withOut(() => gnparse(s"file -s -i $namesInputFilePath -o $nameOutputFilePath"))
        lines should have size 1
        val linesOut = Source.fromFile(nameOutputFilePath.toFile).getLines.toVector
        linesOut should have size 2
        linesOut must contain(exactly(startWith(name1uuid), startWith(name2uuid)))
      }
    }
  }
}
