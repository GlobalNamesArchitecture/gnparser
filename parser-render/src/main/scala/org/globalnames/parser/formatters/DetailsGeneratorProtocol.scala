package org.globalnames
package parser
package formatters

import formatters.{DetailsGenerator => dg}
import spray.json._

object DetailsGeneratorProtocol extends DefaultJsonProtocol {

  implicit val ignoredFormat = jsonFormat1(dg.Ignored)

  implicit val yearFormat = jsonFormat2(dg.Year)

  implicit val subGenusFormat = jsonFormat1(dg.SubGenus)

  implicit val authorsTeamFormat = jsonFormat2(dg.AuthorsTeam)

  implicit val authorsGroupFormat = jsonFormat4(dg.AuthorsGroup)

  implicit val authorshipFormat = jsonFormat3(dg.Authorship)

  implicit val speciesFormat = jsonFormat2(dg.Species)

  implicit val infraSpeciesFormat = jsonFormat3(dg.Infraspecies)

  implicit val uninomialFormat = jsonFormat6(dg.Uninomial)

  implicit val nameFormat = jsonFormat7(dg.Name)

}

