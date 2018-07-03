import os
import sys

gnparser_jar_path = os.environ["GNPARSER_JAR_PATH"]
sys.path.append(gnparser_jar_path)

import json
from org.globalnames.parser import ScientificNameParser

snp = ScientificNameParser.instance()
result = snp.fromString("Homo sapiens L.").jsonRenderer().renderCompactJson()

json_result = json.loads(result)
assert json_result["parsed"]
assert json_result["positions"] == \
        [["genus", 0, 4], ["specific_epithet", 5, 12], ["author_word", 13, 15]]

print "Name is parsed:\n", json_result
