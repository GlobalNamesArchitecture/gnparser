package controllers

import controllers.util.Messages
import org.globalnames.parser.ScientificNameParser
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.mvc._
import ScientificNameParser.{instance => snp}

object Application extends Controller {
  val searchForm = Form("names" -> text)

  private def handleNamesStrings(jsResult: JsResult[Seq[String]]) =
    jsResult match {
      case JsSuccess(ns, _) =>
        val result = ns.map { n =>
          Json.parse(snp.fromString(n).renderCompactJson)
        }
        Ok(obj(Messages.RESULT  -> Messages.RESULT_OK,
               Messages.DETAILS -> result))
      case e: JsError =>
        Ok(obj(Messages.RESULT  -> Messages.RESULT_ERROR,
               Messages.DETAILS -> e.errors.toString))
    }

  def index() = Action {
    Ok(views.html.index(searchForm))
  }

  def batchHandle() = Action(parse.form(searchForm)) { implicit rs =>
    val res =
      if (rs.body.isEmpty) {
        Array.empty[String]
      } else {
        rs.body.split("\r\n")
          .map { name => snp.fromString(name).renderCompactJson }
      }
    Ok(views.html.index(searchForm, rs.body, res))
  }

  def namesGetJson(names: String) = Action {
    val namesJS = Json.parse(names).validate[Seq[String]]
    handleNamesStrings(namesJS)
  }

  def namesPostJson() = Action(parse.json) { implicit rs =>
    val namesJS = rs.body.validate[Seq[String]]
    handleNamesStrings(namesJS)
  }
}
