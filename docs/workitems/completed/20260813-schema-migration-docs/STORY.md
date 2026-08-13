# Story: Schema migration handover docs

**Slug:** `schema-migration-docs`  
**Branch:** `schema-migration-docs`  
**Status:** completed  
**Backlog:** C-15  
**Design:** [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md), [`docs/design/graph/json-schema-to-seeds.md`](../../../design/graph/json-schema-to-seeds.md), [`docs/design/graph/object-schema-dsl.md`](../../../design/graph/object-schema-dsl.md)

## Goal

Give modeling and software teams implementer-grade documentation to produce objs YAML seeds from
JSON Schema knowledge: expand the seed-format reference and a practical **JSON Schema → YAML
seeds** guide (relations covered separately).

## Confirmed decisions

| Topic | Choice |
|-------|--------|
| Seed format doc | Expand existing [`seeds.md`](../../../design/graph/seeds.md) (no parallel format) |
| Comparison / guide | [`json-schema-to-seeds.md`](../../../design/graph/json-schema-to-seeds.md) — practical JS → seeds |
| Guide tone | JSON Schema expert audience; no Excel / legacy-process framing in design docs |
| Converter v1 output | Catalog ontology only (`ObjectSchema` + `AllowedEdgeRule`) |
| Graph seeds | Fully documented in seed reference |
| Product changes | Docs + pasteable Python emitter; no JSON Schema import in product |
| Python helper | [`tools/objs_seed.py`](../../../tools/objs_seed.py) + example + how-to |

## Work Items

- [x] WI-001 — Expand seed-format implementer reference (`WI-001-seed-format-reference.md`)
- [x] WI-002 — JSON Schema ↔ objs comparison (`WI-002-json-schema-comparison.md`)
- [x] WI-003 — Indexes, cross-links, backlog (`WI-003-crosslinks-backlog.md`)
- [x] WI-004 — Python seed nano-framework (`WI-004-python-seed-nano.md`)

## Scope

- Implementer-grade seed field matrices, examples, packaging, generation checklist
- Payload mapping tables + separate relations/edges section + seed production guidance
- Design-folder indexes and backlog row C-15
- Pasteable Python catalog seed emitter + example + how-to

## Out of scope

- Full Excel→seeds product converter / packaging as a pip package
- JSON Schema → DSL product import
- Changing seed runtime format
- `kind: Graph` in the Python nano-framework (v1)
