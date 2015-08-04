package org.globalnames.parser

case class ScientificName(
  verbatim: String = "",
  namesGroup: Option[NamesGroup] = None,
  isVirus: Boolean = false) {

  val isHybrid = namesGroup.map { ng => ng.name.size > 1 || ng.hybrid }
}

case class NamesGroup(
  name: Seq[Name],
  hybrid: Boolean = false,
  quality: Int = 1)

case class Name(
  uninomial: Uninomial,
  subgenus: Option[SubGenus] = None,
  species: Option[Species] = None,
  infraspecies: Option[InfraspeciesGroup] = None,
  comparison: Option[String] = None,
  approximation: Option[String] = None,
  ignored: Option[String] = None,
  quality: Int = 1)

case class Uninomial(
  str: String,
  authorship: Option[Authorship] = None,
  rank: Option[String] = None,
  parent: Option[Uninomial] = None,
  quality: Int = 1)

case class UninomialWord(
  str: String,
  quality: Int = 1)

case class SubGenus(
  subgenus: UninomialWord,
  quality: Int = 1)

case class Species(
  str: String,
  authorship: Option[Authorship] = None,
  quality: Int = 1)

case class Infraspecies(
  str: String,
  rank: Option[String] = None,
  authorship: Option[Authorship],
  quality: Int = 1)

case class InfraspeciesGroup(
  group: Seq[Infraspecies],
  quality: Int = 1)

case class Year(
  str: String,
  quality:  Int = 1)

case class Author(
  str: String,
  anon: Boolean = false,
  filius: Boolean = false,
  quality: Int = 1)

case class AuthorsTeam(
  authors: Seq[Author],
  quality: Int = 1)

case class AuthorsGroup(
  authors: AuthorsTeam,
  authorsEx: Option[AuthorsTeam] = None,
  year: Option[Year] = None,
  quality: Int = 1)

case class Authorship(
  authors: AuthorsGroup,
  combination: Option[AuthorsGroup] = None,
  basionym: Boolean = false,
  quality: Int = 1)
