package org.globalnames.parser.runner
package web
package controllers

import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import models.NamesResponse
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

class WebServerSpec extends WordSpec with Matchers
                    with ScalatestRouteTest with Service {
  "WebServer" should {
    "handle 'GET /'" in {
      Get("/?q=Aus+bus") ~> route ~> check {
        status shouldBe OK
        responseAs[String] should include ("&quot;parsed&quot;: true")
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
