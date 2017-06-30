Version 0.4.1 (2017-06-31)
--------------------------

- support dash in specific epithet (#361)
- migrate to CircleCI 2.0
- minor bug fixes and more test coverage

Version 0.4.0 (2017-06-01)
--------------------------

- support bacteria names parsing (#322)
- support hyphenated genus name with capitalized part (#320)
- support `Oe` unicode character (#321)
- handle lowercase subgenus (#328)
- support input from STDIN and output to STDOUT (#346)
- add `el` as author's prefix (#305)
- support author with `'t` (#252, #255)
- `pv.` (for `pathovar.`) is parsed as rank (#353)
- socket usage (Ruby example) documentation is improved (#355)
- parse `von dem` and `v.` (for `von`) as author prefix (#249, #329)
- names with "satellite(s)" in the end are not parsed as viruses (#246)
- handle `variety` for rank (#233)
- all examples are tested during CircleCI build
- parsing bugs fixes (#332, #330, #327, #326)

Version 0.3.3 (2016-10-05)
--------------------------

- add optionally showing canonical name UUID
- API change: no more AST node ID
- add year range to ast node
- improve benchmarks
- parse names ending on hybrid sign (#88)
- fix: sometimes warning ids are broken (#147)
- hybrid abbreviation expansions (#310)
- raw hybrid formula parsing (#311)
- move to CircleCI
- minor improvements

Version 0.3.2 (2016-07-20)
--------------------------

- speedup performance of lookup translators, regex, string concatenation
- add UUID to parsed canonical

Version 0.3.1 (2016-06-06)
--------------------------

- fix authors' prefix parsing bug (#301)
- add JSON pretty rendering method
- handle punctuation in the end of a name (#302)

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
