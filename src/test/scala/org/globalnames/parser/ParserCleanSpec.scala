package org.globalnames.parser

import org.specs2.mutable.Specification
import scala.util.{Success, Failure}

class ParserCleanSpec extends Specification {
  "ParserClean parses" >> {
    "Betula" in {
      val res = parse("Betula")
      res.isParsed === true
      res.normalized.get === "Betula"
    }
    "Quercus quercus" in {
      val res = parse("Quercus quercus")
      res.normalized === Some("Quercus quercus")
    }
    "Modanthos Alef" in {
      val res = parse("Modanthos Alef")
      res.normalized === Some("Modanthos Alef")
    }
    "Modanthos geranioides Alef." in {
      val res = parse("Modanthos geranioides Alef.")
      res.isParsed === true
      res.normalized === Some("Modanthos geranioides Alef.")
    }
    "Sifangtaiella ganzhaoensis Su 1989" in {
      val res = parse("Sifangtaiella ganzhaoensis Su 1989")
      res.isParsed === true
      res.normalized === Some("Sifangtaiella ganzhaoensis Su 1989")
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
    val pc = new ParserClean(input)
    val parsed = pc.sciName.run()
    parsed match {
      case Success(res: SciName) => res
      case Failure(err) => {
        println(err)
          SciName(input)
      }
      case _ => SciName(input)
    }
  }
}
