Version 1.0.2 (2018-09-25)
--------------------------
- Fix #470: Add logging of parsing time
- Rearrange the codebase: `gnparser` core does not depend on JSON rendering library
- Fix 463045a79: minor issues with multiple `year` node rendering
- Fix cfd9b1d20: JSON snake_case is converted to camelCase
- Fix 86fe367b3: spray-json is used for rendering, that drops redundant conversion in Akka-based servers of `runner`
- Fix #381: cover TcpServiceConnection with Spec
- Fix #471: Limit the size of input stream for CLI to 500K
- README is synchronized with the project current state

Version 1.0.1 (2018-08-06)
--------------------------
- Fix #468, #469: handle out-of-range positions tracking

Version 1.0.0 (2018-07-09)
--------------------------
- Fix #463: the bug with positioning in the result JSON
- Fix #438, #439: redirect service information to STDERR to ease the parsing from STDOUT
- Fix #465: introduce `json_pretty` format flag
- Fix #466: make `file` completely default command
- minor bug fixes

Version 0.4.5 (2018-06-22)
--------------------------
- Fix #462: handle comma in the end of name
- Fix #460: correctly preparse `<i>` tag
- Fix #455, #435: correctly preparse `of` and `not`
- minor bug fixes

Version 0.4.4 (2018-04-10)
--------------------------

- Fix #428: correctly parse `nomenyae`
- Fix #429: don't parse `ex` as infraspecies
- Fix #430: parse `nvar.` rank
- Fix #431: parse "del" as part of authorship
- Fix #432: fix author parsing (e.g. `OS-2017`)
- Fix #433: revert punctuaiton parsing
- Fix #434: support "emend." and "emend"
- Fix #435: parse "& al." as a variant of "et al."
- Fix #436: parse "apud" ("with") as a possible delimiter between authors
- Fix #437: make filius less ambigous and normalise it to "fil." instead of "f."
- minor bug fixes

Version 0.4.3 (2017-12-06)
--------------------------

- Fix #396: don’t parse forbidden strings
- Fix #403: fix viruses patterns
- Fix #404: handle ‘le’ as author prefix
- Fix #402: don’t parse name with ‘of’ word inside (#417)
- Fix #248: handle sp. correctly (#419)
- Fix #405: handle `ht` and `hort`
- Fix #415: don’t parse `spec.` and `nov spec` (#424)
- Fix #408: fix virus pattern (#423)
- Fix #399: fix handling numbers in beginning of word
- Fix #400: don’t parse epithets with ‘.’
- Fix #401: add more ranks and two-letter abbreviations
- Fix #426: cleanup redundant comparison
- improve speed of Util object
- bugs fixes (#380)

Version 0.4.2 (2017-09-28)
--------------------------

- BACKWARD INCOMPATIBILITY: gnparser 0.4.1 optionally returned
  canonical_name.extended in output JSON. Only if input name contains
  ranks, the field contains a canonical name with ranks. Otherwise the field is
  null. gnparser 0.4.2 renames the field to canonical_name.value_ranked. Also
  it always returns it, even if input name doesn't contain ranks.

- provide `EmptyUuid` for empty string in GN namespace
- support `emend` in name (#382)
- correctly parse `str.` for `strain` (#384)
- parse `-x` in species names (#385)
- names with `not` in uninomial/genus are not parsed (#379)
- restrict abbreviated genera to 2 letters (#245)
- minor optimisations, bug fixes (#380)


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
