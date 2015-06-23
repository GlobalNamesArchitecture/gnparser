package org.globalnames.parser

import org.specs2.mutable.Specification
import scala.util.{Success, Failure, Try}

class ParserCleanSpec extends Specification {
  "ParserClean parses" >> {
    "Homo sapiens" in {
      val res = parseName("   Homo   sapiens Linneaus    1758   ")
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
      val res = parseName("Betula")
      res.isParsed must beTrue
      res.normalized.get === "Betula"
      res.canonical.get === "Betula"
    }
    "Quercus quercus" in {
      val res = parseName("Quercus quercus")
      res.normalized === Some("Quercus quercus")
      res.canonical === Some("Quercus quercus")
    }
    "Betula alba" in {
      val res = parseName("Betula alba")
      res.normalized === Some("Betula alba")
      res.canonical === Some("Betula alba")
    }
    "Modanthos Alef" in {
      val res = parseName("Modanthos Alef")
      res.normalized === Some("Modanthos Alef")
      res.canonical === Some("Modanthos")
    }
    "Modanthos geranioides Alef." in {
      val res = parseName("Modanthos geranioides Alef.")
      res.isParsed === true
      res.normalized === Some("Modanthos geranioides Alef.")
      res.canonical === Some("Modanthos geranioides")
    }
    "Sifangtaiella ganzhaoensis Su 1989" in {
      val res = parseName("Sifangtaiella ganzhaoensis Su 1989")
      println(res.toJson)
      res.isParsed === true
      res.normalized === Some("Sifangtaiella ganzhaoensis Su 1989")
      res.canonical === Some("Sifangtaiella ganzhaoensis")
    }
    "Sifangtaiella ganzhaoensis B. de Su 1989" in {
      val res = parseName("Sifangtaiella ganzhaoensis B. de Su 1989")
      res.normalized === Some("Sifangtaiella ganzhaoensis B. de Su 1989")
    }
    "S. ganzhaoensis B. de Dosu 1989" in {
      val res = parseName("S. ganzhaoensis B. de Su 1989")
      res.normalized === Some("S. ganzhaoensis B. de Su 1989")
    }
    "S. ganzhaoensis (B. de Dosu 1989 )" in {
      val res = parseName("S. ganzhaoensis (B. de Su 1989 )")
      res.normalized === Some("S. ganzhaoensis (B. de Su 1989)")
    }
    "S. ganzhaoensis (B. de Dosu 1989 )Someone, 1999" in {
      val res = parseName("S. ganzhaoensis (B. de Su 1989)Someone, 1999")
      res.normalized === Some("S. ganzhaoensis (B. de Su 1989) Someone 1999")
    }
    "Schottera nicaeënsis (J.V. Lamouroux ex Duby) Guiry & Hollenberg" in {
      val res = parseName("Schottera nicaeënsis (J.V. Lamouroux ex Duby) Guiry & Hollenberg")
      res.normalized === Some("Schottera nicaeensis (J. V. Lamouroux ex Duby) Guiry & Hollenberg")
    }
    "Pseudocercospora dendrobii(H.C.  Burnett 1873)U. Braun & Crous ,   2003" in {
      val res = parseName("Pseudocercospora dendrobii(H.C.  Burnett 1873)U. Braun & Crous ,   2003")
      res.normalized === Some("Pseudocercospora dendrobii (H. C. Burnett 1873) U. Braun & Crous 2003")
    }
  }
  "ParserClean does not parse" >> {
    "whateva" in {
      val res = parseName("whateva")
      res.isParsed === false
      res.normalized === None
    }
  }
  def parseName(input: String): SciName = {
    val parser = new ParserClean(input)
    val result = parser.sciName.run()
    SciName.processParsed(input, parser, result)
  }
}
