install.packages("jsonlite", repos="http://cran.r-project.org")

require("rJava")
library(jsonlite)

.jinit(classpath=Sys.getenv('GNPARSER_JAR_PATH'))

snp=J('org.globalnames.parser.ScientificNameParser','instance')
result=snp$fromString("Homo sapiens L.")$renderJsonString(FALSE)
jsonResult=fromJSON(result)

stopifnot(jsonResult$parsed)
stopifnot(jsonResult$canonical_name$value == "Homo sapiens")

print("Name is parsed:")
print(result)
