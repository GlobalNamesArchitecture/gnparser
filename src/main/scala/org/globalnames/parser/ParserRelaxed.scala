// package org.globalnames.parser
//
// import org.parboiled2._
//
// class ParserRelaxed(val input: ParserInput) Parser with RulesClean {
//   override def sciName: Rule1[SciName] = rule {
//     softSpace ~ (nameAuthor | name) ~
//       softSpace ~ EOI ~> ((x: Node) =>
//       SciName(
//         verbatim = input.sliceString(0, input.length),
//         normalized =  Some(x.normalized),
//         canonical = Some(x.canonical),
//         isParsed = true,
//         parserRun = 2
//       )
//     )
//   }
//
//   override private def upperChar = rule {
//     CharPredicate("ABCDEFGHIJKLMNOPQRSTUVWXYZËÆŒ")
//   }
//
//   override private def lowerChar = rule {
//     CharPredicate("abcdefghijklmnopqrstuvwxyzëæœ")
//   }
//
// }
