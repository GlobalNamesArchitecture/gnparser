from org.globalnames.parser import ScientificNameParser

snp = ScientificNameParser.instance()
result = snp.fromString("Homo sapiens L.")
print snp.renderCompactJson(result)
