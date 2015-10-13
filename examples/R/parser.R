require("rJava")
pathToGnParserAssembly='${GnParser_PATH}/gnparser/parser/target/scala-2.11/global-names-parser-assembly-0.1.0-SNAPSHOT.jar'
.jinit(classpath=pathToGnParserAssembly)
snp=J('org.globalnames.parser.ScientificNameParser','instance')
result=snp$fromString("Homo sapiens L.",FALSE)
print(snp$renderCompactJson(result))
