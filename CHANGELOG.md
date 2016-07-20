Version 0.3.2 (2016-07-20)
--------------------------

- speedup performance of lookup translators, regex, string concatenation
- add UUID to parsed canonical

Version 0.3.1 (2016-06-06)
--------------------------

- fix authors' prefix parsing bug (#301)
- add JSON pretty rendering method
- dandle punctuation in the end of a name (#302)

Version 0.3.0 (2016-05-23)
--------------------------

- update parboiled2 dependency. support Scala 2.10.6+ (#275)
- bugfixes and improvements to parser grammar (#87, #89, #232, #236, #237, #239,
#241, #247, #251, #253, #269, #276)
- move `web` project to `runner` (#295)
- become Spark friendly (#280)
- add simplified output form (#281, #282)
- move UUID generation to Utils (#273)
- publish docker right from `runner` (#250)

Version 0.2.0 (2015-11-26)
--------------------------

- support Scala 2.10.3+ (#164)
- simplify API (#176)
- parallel input file parsing (#35)
- clearify JSON fields (#198, #202)
- add JSON schema (#205)
- support dashed authors' names (#218) 

Version 0.1.0 (2015-10-29)
--------------------------

first public release

- porting `biodiversity` gem functionality to Scala
- optimization and speedup of the Scala code
- command line interface
- web server
- REST API
- socket server
- code examples for Java, Scala, Jython, R, JRuby
