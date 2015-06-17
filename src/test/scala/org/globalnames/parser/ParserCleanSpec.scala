package org.globalnames.parser

import org.specs2.mutable.Specification
import scala.util.{Success, Failure}

class ParserCleanSpec extends Specification {
  "ParserClean parses" >> {
    "Betula" in parse("Betula").verbatim === "Betula"
    "Quercus quercus" in parse("Quercus quercus").verbatim === "Quercus quercus"
    "Modanthos Alef" in
    parse("Modanthos Alef")
      .verbatim === "Modanthos Alef"
    "Modanthos geranioides Alef." in
    parse("Modanthos geranioides Alef.")
      .verbatim === "Modanthos geranioides Alef."
    "Sifangtaiella ganzhaoensis Su 1989" in
    parse("Sifangtaiella ganzhaoensis Su 1989")
      .verbatim === "Sifangtaiella ganzhaoensis Su 1989"
  }

  def parse(input: String): ParserClean.Name = {
    val pc = new ParserClean(input)
    val parsed = pc.line.run()
    parsed match {
      case Success(res: ParserClean.Name) => res
      case Failure(err) => {
        println(err)
        ParserClean.Name("")
      }
      case _ => ParserClean.Name("")
    }
  }
}
