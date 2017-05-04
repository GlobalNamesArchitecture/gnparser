package org.globalnames.parser

import org.globalnames.parser.Parser.NodeMeta
import org.parboiled2.{CapturePosition, ParserInput}

import scalaz.{Name => _, _}
import Scalaz._
import scala.io.Source

object FactoryAST {
  private val bacteriaGenera: Set[String] =
    Source.fromURL(getClass.getResource("/bacteria_genera.txt")).getLines
          .map { _.trim }.toSet

  private val bacteriaHomonymsGenera: Set[String] =
    Source.fromURL(getClass.getResource("/bacteria_genera_homonyms.txt")).getLines
          .map { _.trim }.toSet

  def namesGroup(name: NodeMeta[Name],
                 hybridParts: Seq[(HybridChar, Option[NodeMeta[Name]])] = Seq.empty,
                 namedHybrid: Option[HybridChar] = None,
                 bacteria: Boolean = false): NodeMeta[NamesGroup] = {
    val ng = NamesGroup(name.node,
                        hybridParts.map { case (hc, nm) => (hc, nm.map { _.node }) },
                        namedHybrid)
    val warns = name.warnings ++ hybridParts.flatMap{ case (_, nm) => nm.map { _.warnings }.orZero }
    NodeMeta(ng, warns)
  }

  def name(uninomial: NodeMeta[Uninomial],
           subgenus: Option[NodeMeta[SubGenus]] = None,
           species: Option[NodeMeta[Species]] = None,
           infraspecies: Option[NodeMeta[InfraspeciesGroup]] = None,
           comparison: Option[NodeMeta[Comparison]] = None,
           approximation: Option[NodeMeta[Approximation]] = None,
           ignored: Option[String] = None,
           genusParsed: Boolean = false)(implicit input: ParserInput): NodeMeta[Name] = {
    val genus = input.sliceString(uninomial.node.pos.start, uninomial.node.pos.end)
    val bacteria = bacteriaGenera.contains(genus)
    val name = Name(uninomial.node, subgenus.map { _.node }, species.map { _.node },
                    infraspecies.map { _.node }, comparison.map { _.node },
                    approximation.map { _.node }, ignored, bacteria, genusParsed)
    val bacteriaHomonymWarning = bacteriaHomonymsGenera.contains(genus).option {
      Vector(Warning(2, "Genus is bacteria name homonym", name))
    }.getOrElse(Vector())
    val warns = uninomial.warnings ++ subgenus.map { _.warnings }.orZero ++
                species.map { _.warnings }.orZero ++ infraspecies.map { _.warnings }.orZero ++
                comparison.map { _.warnings }.orZero ++ approximation.map { _.warnings }.orZero ++
                bacteriaHomonymWarning
    NodeMeta(name, warns)
  }

  def uninomial(word: NodeMeta[UninomialWord],
                authorship: Option[NodeMeta[Authorship]] = None,
                rank: Option[NodeMeta[Rank]] = None,
                parent: Option[NodeMeta[Uninomial]] = None,
                implied: Boolean = false): NodeMeta[Uninomial] = {
    val unin = Uninomial(word.node, authorship.map { _.node }, rank.map { _.node },
                         parent.map { _.node }, implied)
    val warns = word.warnings ++ authorship.map { _.warnings }.orZero ++
                rank.map { _.warnings }.orZero ++ parent.map { _.warnings }.orZero
    NodeMeta(unin, warns)
  }

  def infraspeciesGroup(group: Seq[NodeMeta[Infraspecies]]): NodeMeta[InfraspeciesGroup] = {
    val ig = InfraspeciesGroup(group.map { _.node })
    val warns = group.flatMap { _.warnings }.toVector
    NodeMeta(ig, warns)
  }

  def infraspecies(word: NodeMeta[SpeciesWord],
                   rank: Option[NodeMeta[Rank]] = None,
                   authorship: Option[NodeMeta[Authorship]]): NodeMeta[Infraspecies] = {
    val inf = Infraspecies(word.node, rank.map { _.node }, authorship.map { _.node })
    val warns = word.warnings ++ rank.map { _.warnings }.orZero ++
                authorship.map { _.warnings }.orZero
    NodeMeta(inf, warns)
  }

  def species(word: NodeMeta[SpeciesWord],
              authorship: Option[NodeMeta[Authorship]] = None): NodeMeta[Species] = {
    val sp = Species(word.node, authorship.map { _.node })
    val warns = word.warnings ++ authorship.map { _.warnings }.orZero
    NodeMeta(sp, warns)
  }

  def comparison(pos: CapturePosition): NodeMeta[Comparison] = {
    val cmp = Comparison(pos)
    NodeMeta(cmp)
  }

  def approximation(pos: CapturePosition): NodeMeta[Approximation] = {
    val appr = Approximation(pos)
    NodeMeta(appr)
  }

  def rank(pos: CapturePosition, typ: Option[String] = None): NodeMeta[Rank] = {
    val rnk = Rank(pos, typ)
    NodeMeta(rnk)
  }

  def subGenus(word: NodeMeta[UninomialWord]): NodeMeta[SubGenus] = {
    val sg = SubGenus(word.node)
    val warns = word.warnings
    NodeMeta(sg, warns)
  }

  def uninomialWord(pos: CapturePosition): NodeMeta[UninomialWord] = {
    val uw = UninomialWord(pos)
    NodeMeta(uw)
  }

  def speciesWord(pos: CapturePosition): NodeMeta[SpeciesWord] = {
    val sw = SpeciesWord(pos)
    NodeMeta(sw)
  }

  def authorship(authors: NodeMeta[AuthorsGroup],
                 combination: Option[NodeMeta[AuthorsGroup]] = None,
                 inparenthesis: Boolean = false,
                 basionymParsed: Boolean = false): NodeMeta[Authorship] = {
    val authorship = Authorship(authors.node, combination.map { _.node },
                                inparenthesis, basionymParsed)
    val warns = authors.warnings ++ combination.map { _.warnings }.orZero
    NodeMeta(authorship, warns)
  }

  def authorsGroup(authors: NodeMeta[AuthorsTeam],
                   authorsEx: Option[NodeMeta[AuthorsTeam]] = None,
                   year: Option[NodeMeta[Year]] = None): NodeMeta[AuthorsGroup] = {
    val ag = AuthorsGroup(authors.node, authorsEx.map { _.node }, year.map { _.node })
    val warns = authors.warnings ++ authorsEx.map { _.warnings }.orZero ++
                year.map { _.warnings }.orZero
    NodeMeta(ag, warns)
  }

  def authorsTeam(authors: Seq[NodeMeta[Author]]): NodeMeta[AuthorsTeam] = {
    val at = AuthorsTeam(authors.map { _.node })
    val warns = authors.flatMap { _.warnings }.toVector
    NodeMeta(at, warns)
  }

  def author(words: Seq[NodeMeta[AuthorWord]],
             anon: Boolean = false,
             filius: Option[NodeMeta[AuthorWord]] = None): NodeMeta[Author] = {
    val au = Author(words.map { _.node }, anon, filius.map { _.node })
    val warns = words.flatMap { _.warnings } ++ filius.map { _.warnings }.orZero
    NodeMeta(au, warns.toVector)
  }

  def authorWord(pos: CapturePosition,
                separator: AuthorWordSeparator = AuthorWordSeparator.None): NodeMeta[AuthorWord] = {
    val aw = AuthorWord(pos, separator)
    NodeMeta(aw)
  }

  def year(pos: CapturePosition): NodeMeta[Year] = {
    val yr = Year(pos)
    NodeMeta(yr)
  }
}