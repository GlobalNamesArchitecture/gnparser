package org.globalnames
package parser
package runner.web.models

import spray.json.JsValue

case class NamesRequest(names: Seq[String])

case class NamesResponse(namesJson: Seq[formatters.Summarizer.Summary])
