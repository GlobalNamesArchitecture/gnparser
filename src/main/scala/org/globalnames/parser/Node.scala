package org.globalnames.parser

import scala.collection._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

case class Node(
  normalized: String = "",
  canonical: String = "",
  hybrid: Boolean = false,
  parser_run: Int = 1,
  details: Option[Vector[Name]] = None,
  pos: Option[Vector[Tuple3[Int, Int, String]]] = None
)

trait Details

case class NamesGroup(
  name: Seq[Name],
  hybrid: Boolean = false,
  quality: Int = 1) extends Details

case class Name(
  uninomial: Uninomial,
  subgenus: Option[SubGenus] = None,
  species: Option[Species] = None,
  infraspecies: Option[InfraspeciesGroup] = None,
  comparison: Option[String] = None,
  approximation: Option[String] = None,
  ignored: Option[String] = None,
  quality: Int = 1) extends Details

case class Uninomial(
  str: String,
  authorship: Option[Authorship],
  rank: Option[String] = None,
  parent: Option[Uninomial] = None,
  quality: Int = 1) extends Details

case class Subgenus(
  subgenus: Uninomial,
  quality: Int = 1) extends Details

case class UninomialWord(
  str: String,
  quality: Int = 1) extends Details

case class Species(
  str: String,
  authorship: Option[Authorship] = None,
  quality: Int = 1) extends Details

case class Infraspecies(
  str: String,
  rank: Option[String] = None,
  authorship: Option[Authorship],
  quality: Int = 1) extends Details

case class InfraspeciesGroup(
  group: Seq[Infraspecies],
  quality: Int = 1) extends Details

case class Year(
  str: String,
  quality:  Int = 1) extends Details

case class Author(
  str: String,
  anon: Boolean = false,
  quality: Int = 1) extends Details

case class AuthorsTeam(
  authors: Seq[Author],
  quality: Int = 1) extends Details

case class AuthorsGroup(
  authors: AuthorsTeam,
  authorsEx: Option[AuthorsTeam] = None,
  year: Option[Year] = None,
  quality: Int = 1) extends Details

case class Authorship(
  authors: AuthorsGroup,
  combination: Option[AuthorsGroup] = None,
  quality: Int = 1) extends Details
