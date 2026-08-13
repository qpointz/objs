# WI-002 — JSON Schema → YAML seeds guide

**Status:** done  
**Story:** [`STORY.md`](STORY.md)

## Goal

Write [`docs/design/graph/json-schema-to-seeds.md`](../../../design/graph/json-schema-to-seeds.md)
as a practical guide for JSON Schema practitioners producing objs seed YAML: payload mapping,
separate relations section, production guidance. No Excel / legacy-process framing.

## Acceptance

- [x] Three surfaces clarified (DSL, seed ObjectSchema, JSON Schema projection)
- [x] Field-by-field payload mapping + unsupported constructs
- [x] Relations as `AllowedEdgeRule` (not JSON Schema `$ref` properties)
- [x] Catalog seed production / validation / non-goals section
- [x] Design docs free of Excel / customer-process narration
