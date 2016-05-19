package org.globalnames.parser
package runner.web
package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import spray.json._
import spray.json.DefaultJsonProtocol
import views.{MenuItem, MenuItemContent, html}
import models.{NamesRequest, NamesResponse}
import ScientificNameParser.{instance => snp}
import akkahttptwirl.TwirlSupport._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

trait Protocols extends DefaultJsonProtocol {
  implicit val nameRequestFormat   = jsonFormat1(NamesRequest.apply)
  implicit val nameResponseFormat = jsonFormat1(NamesResponse.apply)
}

object WebServer extends Protocols {
  private val (indexMenu, parserMenu) = {
    val prs = MenuItemContent("Parser", "/")
    val api = MenuItemContent("API", "/doc/api")

    val indexMenu = Seq(MenuItem(prs, current = true),
      MenuItem(api, current = false))
    val apiMenu   = Seq(MenuItem(prs, current = false),
      MenuItem(api, current = true))
    (indexMenu, apiMenu)
  }

  private def parseNames(names: Seq[String]) = {
    val namesJson = names.map { name =>
      snp.fromString(name).renderCompactJson.parseJson
    }
    namesJson
  }

  val route =
    pathSingleSlash {
      (get & parameter('q ?)) { namesReq =>
        complete {
          val inputNames = namesReq.map { _.split("\r\n") }.getOrElse(Array())
          val parsedNames = parseNames(inputNames).map { _.prettyPrint }
          html.index(namesReq.getOrElse(""), parsedNames, indexMenu)
        }
      }
    } ~
    (path("doc" / "api") & get) {
      complete {
        html.api(parserMenu)
      }
    } ~
    path("api") {
      (get & parameter('q ?)) { namesReq =>
        complete {
          val inputNames = namesReq.map { _.split('|') }.getOrElse(Array())
          NamesResponse(parseNames(inputNames))
        }
      } ~
      (post & entity(as[Seq[String]])) { namesReq =>
        complete {
          NamesResponse(parseNames(namesReq))
        }
      }
    } ~
    pathPrefix("public") {
      getFromResourceDirectory("public")
    }

  def run(port: Int): Unit = {
    implicit val system = ActorSystem("global-names-web-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    Http().bindAndHandle(route, "localhost", port)
    println(s"Server online at http://localhost:$port/")
  }

  def main(args: Array[String]): Unit =
    run(8080)
}
