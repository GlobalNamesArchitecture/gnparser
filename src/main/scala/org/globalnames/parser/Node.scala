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

trait Details {
}

case class Name(
  uninomial: Uninomial,
  species: Option[Species] = None,
  infraspecies: Option[Vector[Infraspecies]] = None
) extends Details {
  def prepare = {
    ("uninomial" -> ("string" -> uninomial.string))
  }
}

case class Uninomial(string: String) extends Details
case class Species(string: String) extends Details
case class Infraspecies(string: String) extends Details
