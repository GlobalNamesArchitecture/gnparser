package org.globalnames.parser

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.specs2.mutable.Specification

import scala.io.Source
import scalaz.std.string._
import scalaz.syntax.std.option._

class ScientificNameParserSpec extends Specification {
  case class ExpectedName(verbatim: String, json: String, simple: String)

  "ScientificNameParser specification".p

  val scientificNameParser = new ScientificNameParser {
    val version = "test_version"
  }

  def expectedNames(filePath: String): Vector[ExpectedName] = {
    Source.fromURL(getClass.getResource(filePath), "UTF-8")
          .getLines
          .takeWhile { _.trim != "__END__" }
          .withFilter { line => !(line.isEmpty || ("#\r\n\f\t" contains line.charAt(0))) }
          .sliding(3, 3)
          .map { ls => ExpectedName(ls(0), ls(1), ls(2)) }
          .toVector
  }

  expectedNames("/test_data.txt").foreach { expectedName =>
      val json = parse(expectedName.json)
      val jsonParsed = scientificNameParser.fromString(expectedName.verbatim).json()
                         .removeField { case (_, v) => v == JNothing }

      val jsonDiff = {
        val Diff(changed, added, deleted) = jsonParsed.diff(json)
        s"""line:
           |${expectedName.verbatim}
           |parsed:
           |${pretty(jsonParsed)}
           |test_data:
           |${pretty(json)}
           |changed:
           |${pretty(changed)}
           |added:
           |${pretty(added)}
           |deleted:
           |${pretty(deleted)}""".stripMargin
      }

      s"parse correctly: '${expectedName.verbatim}'" in {
        s"original json must match expected one:\n $jsonDiff" ==> {
          json === jsonParsed
        }
      }

      val Array(uuid, verbatim, canonical, canonicalExtended,
                authorship, year, quality) = expectedName.simple.split('|')

      s"parse correctly delimited string: '${expectedName.verbatim}'" in {
        val pr = scientificNameParser.fromString(expectedName.verbatim)

        uuid              === pr.input.id.toString
        verbatim          === pr.input.verbatim
        canonical         === pr.canonized().orZero
        canonicalExtended === pr.canonized(showRanks = true).orZero
        authorship        === pr.authorshipDelimited.orZero
        year              === pr.yearDelimited.orZero
        quality.toInt     === pr.scientificName.quality
      }

      s"contain no duplicates in warnings" in {
        val pr = scientificNameParser.fromString(expectedName.verbatim)
        Set(pr.warnings: _*).size === pr.warnings.size
      }

      s"contain no orphans in warnings" in {
        def hasRefNode(sourceNode: AstNode, targetNode: AstNode): Boolean = targetNode match {
          case tn if tn == sourceNode => true
          case sn: ScientificName => sn.namesGroup.exists { ng => hasRefNode(sourceNode, ng) }
          case ng: NamesGroup =>
            hasRefNode(sourceNode, ng.name) ||
            ng.hybridParts.exists { case (hc, name) =>
              hasRefNode(sourceNode, hc) || name.exists { n => hasRefNode(sourceNode, n) }
            }
          case n: Name => hasRefNode(sourceNode, n.uninomial) ||
                          n.subgenus.exists { sg => hasRefNode(sourceNode, sg) } ||
                          n.species.exists { sp => hasRefNode(sourceNode, sp) } ||
                          n.infraspecies.exists { is => hasRefNode(sourceNode, is) } ||
                          n.comparison.exists { cmp => hasRefNode(sourceNode, cmp) } ||
                          n.approximation.exists { aprx => hasRefNode(sourceNode, aprx) }
          case sg: SubGenus => hasRefNode(sourceNode, sg.word)
          case u: Uninomial => u.authorship.exists { auth => hasRefNode(sourceNode, auth)} ||
                               u.rank.exists { rk => hasRefNode(sourceNode, rk) } ||
                               u.parent.exists { par => hasRefNode(sourceNode, par) }
          case ig: InfraspeciesGroup => ig.group.exists { is => hasRefNode(sourceNode, is) }
          case is: Infraspecies => hasRefNode(sourceNode, is.word) ||
                                   is.rank.exists { rk => hasRefNode(sourceNode, rk) } ||
                                   is.authorship.exists { a => hasRefNode(sourceNode, a) }
          case sp: Species => hasRefNode(sourceNode, sp.word) ||
                              sp.authorship.exists { a => hasRefNode(sourceNode, a) }
          case auth: Authorship => hasRefNode(sourceNode, auth.authors) ||
                                   auth.combination.exists { cmb => hasRefNode(sourceNode, cmb) }
          case ag: AuthorsGroup => hasRefNode(sourceNode, ag.authors) ||
                                   ag.authorsEx.exists { auEx => hasRefNode(sourceNode, auEx) } ||
                                   ag.year.exists { y => hasRefNode(sourceNode, y) }
          case at: AuthorsTeam => at.authors.exists { a => hasRefNode(sourceNode, a) }
          case au: Author => au.words.exists { aw => hasRefNode(sourceNode, aw) } ||
                             au.filius.exists { f => hasRefNode(sourceNode, f) }
          case _ => false
        }

        val pr = scientificNameParser.fromString(expectedName.verbatim)

        pr.warnings.filterNot {
          warning => hasRefNode(warning.node, pr.scientificName)
        } should beEmpty
      }
  }
}
