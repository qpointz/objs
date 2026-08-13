# WI-004 — Python seed nano-framework

**Status:** done  
**Story:** [`STORY.md`](STORY.md)

## Goal

Add pasteable stdlib-only [`tools/objs_seed.py`](../../../tools/objs_seed.py) plus runnable
[`tools/objs_seed_example.py`](../../../tools/objs_seed_example.py) (entities + edge variants,
`identifier` / `searchable`) and document usage in
[`json-schema-to-seeds.md`](../../../design/graph/json-schema-to-seeds.md).

## Acceptance

- [x] Single-file Catalog / ObjectSchema / EdgeRule / field helpers; YAML emit; no third-party deps
- [x] Scalar `identifier` + `searchable` flags
- [x] Example covers NONE, SCHEMA, cardinality, wildcard edges
- [x] How-to section + seeds.md pointer
