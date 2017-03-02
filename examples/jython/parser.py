from org.globalnames.parser import ScientificNameParser
import json

snp = ScientificNameParser.instance()
result = snp.fromString("Homo sapiens L.").renderCompactJson()

json_result = json.loads(result)
assert json_result["parsed"]
assert json_result["positions"] == \
        [["genus", 0, 4], ["specific_epithet", 5, 12], ["author_word", 13, 15]]
