import os
import sys

gnparser_jar_path = os.environ["GNPARSER_JAR_PATH"]
sys.path.append(gnparser_jar_path)

import json
from org.globalnames.parser import ScientificNameParser

snp = ScientificNameParser.instance()
result = snp.fromString("Homo sapiens L.").renderJsonString(False)

json_result = json.loads(result)
assert json_result["parsed"]
assert json_result["positions"] == \
        [["genus", 0, 4], ["specificEpithet", 5, 12], ["authorWord", 13, 15]]

print "Name is parsed:\n", json_result
