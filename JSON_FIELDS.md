# JSON Fields from Parsed Results

The document informally describes the structure of JSon produced by gnparser.

| Field | Parents | Type | Description |
|-------|---------|------|-------------|
| scientific_name | | Dictionary | Root field for the whole record |
| parsed | scientific_name | Boolean | Indicates if the parser succeeded of failed |
| quality | scientific_name | Enumeration | Indicates how well a name was formed : 1 - no problems during parsing, 2 - small inconsistencies, 3 - larger problems |
| quality_warnings | scientific_name | Array | Presents all warnings and their 'quality value' if quality field equal 2 or 3 |
| virus | scientific_name | Boolean | True for viruses, false for others |
| surrogate | scientific_name | Boolean | True for "surrogate" names which are only partially identified |
| hybrid | scientific_name | Boolean | True for any kind of hybrid |
| parser_version | scientific_name | String | Version of the parser artefact |
| verbatim | scientific_name | String | Unmodified input name string |
| normalized | scientific_name | String | 'Cleaned up' input name string|
| canonical | scientific_name | String | Extracted most stable components of the name (canonical form) |
| canonical_extended | scientific_name | String |  Infraspecific ranks are added to canonical form |
| positions | scientific_name | Array | Two-dimensional array where each element contains data for `semantic meaning of a word`, the `start` index and `end` index of a word |
| details | scientific_name | Dictionary | Contains all parsed details of a name |
| annotation_identification | details | String | Marks type of a partial identification of a  specimen |
| ignored | details | String | String after annotation_identification |
| garbage | details | String | Non-parseable tail of the name string |
| uninomial | details | Dictionary | Data for a stand-alone uninomial (subgenus and higher) |
| species | details | Dictionary | Data for species epithet |
| infraspecies | details | Array | Infraspecific data |
| string    | uninomial, species, infraspecies | String | Normalized word for the taxon |
| authorship | uninomial, species, infraspecies | String | Normalized string representing complete authorship for a taxon |
| rank | uninomial, infraspecies | String | Rank either for combination from 2 uninomials, or an infraspecific rank |
| basionym_author_team    | uninomial, species, infraspecies | Dictionary | Data about the apparent original authors of the taxon |
| combination_author_team | uninomial, species, infraspecies | Dictionary | Data about apparent combination authors of the taxon |
| author | basionym_author_team, combination_author_team | Array | Array of normalized author strings |
| year | basionym_author_team, combination_author_team | Dictionary | Data about the year of publication |
| str | year | String | Year value |
| approximate | year | Boolean | True if year is marked as approximate |

