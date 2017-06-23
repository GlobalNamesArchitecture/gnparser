package org.globalnames.parser.runner
package web
package controllers

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import models.NamesResponse
import org.scalatest.{Matchers, WordSpec}

class WebServerSpec extends WordSpec with Matchers
                    with ScalatestRouteTest with Service {
  "WebServer" should {
    "handle 'GET /'" in {
      Get("/?q=Aus+bus") ~> route ~> check {
        status shouldBe OK
        val response = responseAs[String]
        withClue("has no `parsed: true` in result") {
          response should include("&quot;parsed&quot;: true")
        }
        withClue("has no input name in result") {
          response should include("&quot;verbatim&quot;: &quot;Aus bus&quot;,")
        }
        withClue("has `Parse` button on page") {
          response should include("""<input type="submit" value="Parse"/>""")
        }
      }
    }

    "handle 'GET /api'" in {
      Get("/api?q=Aus+bus") ~> route ~> check {
        status shouldBe OK
        contentType shouldEqual `application/json`
        responseAs[NamesResponse].namesJson should have size 1
      }
    }

    "handle 'POST /api'" in {
      Post("/api", HttpEntity(`application/json`, "[\"Aus bus\"]")) ~>
        route ~> check {
        status shouldBe OK
        contentType shouldEqual `application/json`
        responseAs[NamesResponse].namesJson should have size 1
      }
    }

    "handle 'GET /doc/api'" in {
      Get("/doc/api") ~> route ~> check {
        status shouldBe OK
        responseAs[String] should include ("(API)")
      }
    }
  }
}
