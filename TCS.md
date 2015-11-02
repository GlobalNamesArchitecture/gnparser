Changes in terms for JSON fields

We are using modified [TCS terms](http://www.tdwg.org/fileadmin/_migrated/content_uploads/v101.xsd)

TODO: Delete this file after chages in tests

scientific_name - remove
quality - no change
quality_warnings - no change
virus - no change
surrogate - no change
hybrid - no change
parser_version - no change
verbatim - no change
normalized - no change
canonical_name - instead of canonical. Full format:
"canonical_name": {
  "value": "Homo sapiens sapiens",
  "extended: "Homo sapiens ssp. sapiens",
}
extended - instead of canonical_extended (see above) -- OPTIONAL
positions - no change
details - no change
annotation_identification - no change
ignored - no change
garbage - no change
uninomial - no change

specific_epithet - instead of species:
"specific_epithet": {
  "value": "sapiens",
  ...
}

infraspecific_epithets - instead of infraspecies
"infraspecific_epithets": [...]

"value" - instead of "string", "str"

authorship -  no change except 'terminal' authorship can be like
"authorship": {
  "value": "(Linnaeus 1799) Mozzherin 2008",
  "canonical_authorship" = true
  "basionym_authorshp": { "authors": ["Linnaeus"], ...}
  "combination_authorship": { "authors": ... }
  }

rank - no change
basionym_authorship - instead of basionym_author_team
combination_authorship - instead of combination_author_team
authors - instead of author
year - no change but
"year": {"value": "1888"}
approximate - no change OPTIONAL
