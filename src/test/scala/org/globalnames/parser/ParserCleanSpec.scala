package org.globalnames.parser

import org.specs2.mutable.Specification
import scala.util.{Success, Failure}

class ParserCleanSpec extends Specification {
  "ParserClean parses" >> {
    "Homo sapiens" in {
      val res = parse("   Homo   sapiens Linneaus    1758   ")
      res.verbatim === "   Homo   sapiens Linneaus    1758   "
      res.normalized === Some("Homo sapiens Linneaus 1758")
      res.canonical === Some("Homo sapiens")
      res.isParsed must beTrue
      res.isVirus must beFalse
      res.isHybrid must beFalse
      res.id === "682f477a-a3dc-57ee-9524-77f3cc2b2feb"
      res.parserVersion must =~("""^\d+\.\d+\.\d+(-SNAPSHOT)?$""")
    }
    "Betula" in {
      val res = parse("Betula")
      res.isParsed must beTrue
      res.normalized.get === "Betula"
      res.canonical.get === "Betula"
    }
    "Quercus quercus" in {
      val res = parse("Quercus quercus")
      res.normalized === Some("Quercus quercus")
      res.canonical === Some("Quercus quercus")
    }
    "Modanthos Alef" in {
      val res = parse("Modanthos Alef")
      res.normalized === Some("Modanthos Alef")
      res.canonical === Some("Modanthos")
    }
    "Modanthos geranioides Alef." in {
      val res = parse("Modanthos geranioides Alef.")
      res.isParsed === true
      res.normalized === Some("Modanthos geranioides Alef.")
      res.canonical === Some("Modanthos geranioides")
    }
    "Sifangtaiella ganzhaoensis Su 1989" in {
      val res = parse("Sifangtaiella ganzhaoensis Su 1989")
      println(res.toJson)
      res.isParsed === true
      res.normalized === Some("Sifangtaiella ganzhaoensis Su 1989")
      res.canonical === Some("Sifangtaiella ganzhaoensis")
    }
  }
  "ParserClean does not parse" >> {
    "whateva" in {
      val res = parse("whateva")
      res.isParsed === false
      res.normalized === None
    }
  }

  def parse(input: String): SciName = {
    val parser = new ParserClean(input)
    val result = parser.sciName.run()
    SciName.processParsed(input, parser, result)
  }
}
