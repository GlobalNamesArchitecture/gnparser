package org.globalnames.parser.runner

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets

import GnParser.{Config, InputFileParsing}
import org.specs2.mutable.Specification
import org.specs2.matcher.MatcherMacros

class GnParserSpec extends Specification with MatcherMacros {
  "GnParserSpec" >> {
    "should parse from file by default" >> {
      GnParser.parse(Array()).get must matchA[Config].mode(Some(InputFileParsing))
    }

    "should have correct version" >> {
      GnParser.gnParserVersion must_== org.globalnames.parser.BuildInfo.version
    }

    def withInOut(input: String, thunk: () => Unit): Array[String] = {
      val streamIn = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))
      val streamOut = new ByteArrayOutputStream()
      Console.withIn(streamIn) {
        Console.withOut(streamOut) {
          thunk()
          streamOut.toString(StandardCharsets.UTF_8.displayName).split("\n")
        }
      }
    }

    "should parse from <stdin> to <stdout> when no input provided" >> {
      val input = """Homo sapiens
                    |Salinator solida""".stripMargin
      val lines = withInOut(input, () => GnParser.main(Array("-s"))).takeRight(2)
      lines(0) startsWith "16f235a0-e4a3-529c-9b83-bd15fe722110"
      lines(1) startsWith "da1a79e5-c16f-5ff7-a925-14c5c7ecdec5"
    }

    "should parse single name" >> {
      val input = "Homo sapiens"
      val lines = withInOut(input, () => GnParser.main(Array("-s"))).takeRight(1)
      lines(0) startsWith "16f235a0-e4a3-529c-9b83-bd15fe722110"
    }
  }
}
