require "java"
require "json"
java_import "org.globalnames.parser.ScientificNameParser"

snp = ScientificNameParser.instance
result = JSON.parse(snp.fromString("Homo sapiens L.").renderJsonString(false))

raise "not parsed" unless result["parsed"]
raise "positions are wrong" unless
    result["positions"] == [["genus", 0, 4], ["specificEpithet", 5, 12], ["authorWord", 13, 15]]

puts "Name is parsed:\n #{result}"
