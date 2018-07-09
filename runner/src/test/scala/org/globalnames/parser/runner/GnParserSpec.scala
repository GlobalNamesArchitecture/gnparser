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

    def withInOut(input: String, thunk: () => Unit): (Array[String], Array[String]) = {
      val streamIn = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))
      Console.withIn(streamIn) { withOut(thunk) }
    }

    def withOut(thunk: () => Unit): (Array[String], Array[String]) = {
      def extractLinesFromStream(baos: ByteArrayOutputStream): Array[String] = {
         val str = baos.toString(StandardCharsets.UTF_8.displayName)
         str.split("\n").filterNot { x => x == null || x == "" }
      }

      java.lang.System.setErr(java.lang.System.out)
      val streamOut = new ByteArrayOutputStream()
      val streamErr = new ByteArrayOutputStream()
      Console.withOut(streamOut) {
        Console.withErr(streamErr) {
          thunk()
          val outStr = extractLinesFromStream(streamOut)
          val errStr = extractLinesFromStream(streamErr)
          (outStr, errStr)
        }
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
        val (lines, errors) = withInOut("", () => gnparse("file -f simple"))
        lines should beEmpty

        errors should have size 2
        errors(0) should_== GnParser.welcomeMessage
        errors(1) must startWith("Running with parallelism")
      }

      "should parse single name from <stdin>" >> {
        val (lines, _) = withInOut(name1, () => gnparse("file -f simple"))
        lines should have size 1
        lines(0) must startWith(name1uuid)
      }

      "should parse from <stdin> to <stdout> when no file is provided" >> {
        val input = s"""$name1
                       |$name2""".stripMargin
        val (lines, _) = withInOut(input, () => gnparse("file -f simple"))
        lines should have size 2
        lines.toSeq should contain(exactly(startWith(name1uuid), startWith(name2uuid)))
      }

      "should parse from input file to <stdout>" >> {
        val (lines, _) = withOut(() => gnparse(s"file -f simple -i $namesInputFilePath"))
        lines should have size 2
        lines.toSeq should contain(exactly(startWith(name1uuid), startWith(name2uuid)))
      }

      "should parse from <stdin> to output file" >> {
        val input = s"""$name1
                       |$name2""".stripMargin
        val nameOutputFilePath = Files.createTempFile("names_output1", ".txt")
        val (lines, _) = withInOut(input, () => gnparse(s"file -f simple -o $nameOutputFilePath"))
        lines should beEmpty

        val linesOut = Source.fromFile(nameOutputFilePath.toFile).getLines.toVector
        linesOut should have size 2
        linesOut should contain(exactly(startWith(name1uuid), startWith(name2uuid)))
      }

      "should parse from input file to output file" >> {
        val nameOutputFilePath = Files.createTempFile("names_output2", ".txt")
        val (lines, _) = withOut { () =>
          gnparse(s"file -f simple -i $namesInputFilePath -o $nameOutputFilePath")
        }

        lines should beEmpty

        val linesOut = Source.fromFile(nameOutputFilePath.toFile).getLines.toVector
        linesOut should have size 2
        linesOut should contain(exactly(startWith(name1uuid), startWith(name2uuid)))
      }
    }
  }
}
