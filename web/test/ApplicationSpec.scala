import controllers.util.Messages
import org.specs2.mutable._
import play.api.libs.json.Json._
import play.api.libs.json._

import play.api.test._
import play.api.test.Helpers._

class ApplicationSpec extends Specification {

  "Application" should {
    "handle 'GET /api'" in new WithApplication {
      val api = route(FakeRequest(GET, """/api?names=["Aus bus", "Aus bus 1950"]""")).get
      status(api) must equalTo(OK)
      contentType(api) must beSome.which { _ == "application/json" }
      (contentAsJson(api) \ Messages.RESULT).as[String] must_== Messages.RESULT_OK
      (contentAsJson(api) \ Messages.DETAILS).as[Seq[JsObject]] must haveSize(2)
    }

    "handle 'POST /api'" in new WithApplication {
      val request = FakeRequest(POST, "/api").withJsonBody(arr("Aus bus", "Aus bus 1950"))
      val api = route(request).get
      status(api) must equalTo(OK)
      contentType(api) must beSome.which { _ == "application/json" }
      (contentAsJson(api) \ Messages.RESULT).as[String] must_== Messages.RESULT_OK
      (contentAsJson(api) \ Messages.DETAILS).as[Seq[JsObject]] must haveSize(2)
    }

    "handle form submission" in new WithApplication {
      val request = FakeRequest(POST, "/")
                     .withFormUrlEncodedBody("names" -> "Aus bus\nAus bus 1950")
      val index = route(request).get
      status(index) must equalTo(OK)
      contentType(index) must beSome.which { _ == "text/html" }
      contentAsString(index) must contain("scientific_name")
    }
  }
}
