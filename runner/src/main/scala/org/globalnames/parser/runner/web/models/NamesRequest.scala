package org.globalnames.parser.runner.web.models

import spray.json.JsValue

case class NamesRequest(names: Seq[String])

case class NamesResponse(namesJson: Seq[JsValue])
