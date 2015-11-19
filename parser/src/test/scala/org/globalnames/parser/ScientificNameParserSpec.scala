package org.globalnames.parser

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.specs2.mutable.Specification

import scala.io.Source

class ScientificNameParserSpec extends Specification {

  "ScientificNameParser specification".p

  val lines = Source.fromURL(getClass.getResource("/test_data.txt"), "UTF-8").getLines
  val scientificNameParser = new ScientificNameParser {
    val version = "test_version"
  }

  val jsonSchemaValidator = {
    val resource = getClass.getResource("/gnparser.json")
    val gnParserJsonSchema = Source.fromURL(resource, "UTF-8").mkString
    val mapper = new ObjectMapper()
    val factory = mapper.getFactory
    val jsonParser = factory.createParser(gnParserJsonSchema)
    val schemaJson: JsonNode = mapper.readTree(jsonParser)
    JsonSchemaFactory.byDefault().getJsonSchema(schemaJson)
  }

  for (line <- lines.takeWhile { _.trim != "__END__" } if !(line.isEmpty || ("#\r\n\f\t" contains line.charAt(0)))) {
    val Array(inputStr, expectedJsonStr) = line.split('|')

    val json = parse(expectedJsonStr)
    val parsedResult = scientificNameParser.fromString(inputStr)
    val jsonParsed = parsedResult.json.removeField { case (_, v) => v == JNothing }
    val jsonDiff = {
      val Diff(changed, added, deleted) = jsonParsed.diff(json)
      s"""Line:
         |$line
         |Original:
         |${pretty(jsonParsed)}
         |Expected:
         |${pretty(json)}
         |Changed:
         |${pretty(changed)}
         |Added:
         |${pretty(added)}
         |Deleted:
         |${pretty(deleted)}""".stripMargin
    }

    val jsonObj: JsonNode = {
      val mapper = new ObjectMapper()
      val factory = mapper.getFactory
      val jsonParser = factory.createParser(parsedResult.renderCompactJson)
      mapper.readTree(jsonParser)
    }
    val report = jsonSchemaValidator.validate(jsonObj)

    s"must parse: '$inputStr'" in {
      s"does not match expected one:\n $jsonDiff" ==> {
        json === jsonParsed
      }

      s"does not conform with schema:\n ${report.toString}" ==> {
        report.isSuccess must beTrue
      }
    }
  }
}
