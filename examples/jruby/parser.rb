require "java"
require "json"
java_import "org.globalnames.parser.ScientificNameParser"

snp = ScientificNameParser.instance
result = JSON.parse(snp.fromString("Homo sapiens L.").renderCompactJson)

raise "not parsed" unless result["parsed"]
raise "positions are wrong" unless
    result["positions"] == [["genus", 0, 4], ["specific_epithet", 5, 12], ["author_word", 13, 15]]

puts "Name is parsed:\n #{result}"
