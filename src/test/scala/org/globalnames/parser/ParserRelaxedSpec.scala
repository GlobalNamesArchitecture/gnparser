package org.globalnames.parser

import org.specs2.mutable.Specification
import scala.util.{Success, Failure}

// class ParserRelaxedSpec extends Specification {
//   "ParserClean parses" >> {
//     "Döringina Ihering 1929" in {
//       val res = SciName.fromString("Döringina Ihering 1929")
//       res.verbatim === "Döringina Ihering 1929"
//       res.normalized === Some("Döringina Ihering 1929")
//       res.canonical === Some("Döringina")
//       res.isParsed must beTrue
//       res.isVirus must beFalse
//       res.isHybrid must beFalse
//       res.id === "682f477a-a3dc-57ee-9524-77f3cc2b2feb"
//       res.parserVersion must =~("""^\d+\.\d+\.\d+(-SNAPSHOT)?$""")
//     }
//   }
// }
