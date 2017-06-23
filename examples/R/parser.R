install.packages("jsonlite", repos="http://cran.r-project.org")

require("rJava")
library(jsonlite)

pathToGnParserAssembly='parser/target/scala-2.11/gnparser-assembly-0.4.1-SNAPSHOT.jar'
.jinit(classpath=pathToGnParserAssembly)

snp=J('org.globalnames.parser.ScientificNameParser','instance')
result=snp$fromString("Homo sapiens L.")$renderCompactJson()
jsonResult=fromJSON(result)

stopifnot(jsonResult$parsed)
stopifnot(jsonResult$canonical_name$value == "Homo sapiens")
