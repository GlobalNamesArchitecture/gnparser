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
import views.{MenuItemContent, MenuItem}

object Application extends Controller {
  val searchForm = Form("names" -> text)
  private val (indexMenu, parserMenu) = {
    val prs = MenuItemContent("Parser", routes.Application.index())
    val api = MenuItemContent("API", routes.Application.api())

    val indexMenu = Seq(MenuItem(prs, current = true),
                        MenuItem(api, current = false))
    val apiMenu   = Seq(MenuItem(prs, current = false),
                        MenuItem(api, current = true))
    (indexMenu, apiMenu)
  }

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
    Ok(views.html.index(menu = indexMenu))
  }

  def api() = Action {
    Ok(views.html.api(menu = parserMenu))
  }

  def batchHandle() = Action(parse.form(searchForm)) { implicit rs =>
    val res =
      if (rs.body.isEmpty) {
        Array.empty[String]
      } else {
        rs.body.split("\r\n").filter { _.nonEmpty }
          .map { name => snp.fromString(name).renderCompactJson }
      }
    Ok(views.html.index(rs.body, res, menu = indexMenu))
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
