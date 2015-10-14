require "java"
java_import "org.globalnames.parser.ScientificNameParser"

snp = ScientificNameParser.instance
result = snp.fromString("Homo sapiens L.")
puts snp.renderCompactJson(result)
