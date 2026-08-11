# Story: Schema field identifiers (and related flags)

**Slug:** `schema-field-identifiers`  
**Branch:** `schema-field-identifiers` (track `origin/schema-field-identifiers`)  
**Status:** closed  
**Archive:** [`docs/workitems/completed/20260811-schema-field-identifiers/`](.)  
**Backlog:** [C-14](../../BACKLOG.md)  
**Base:** `origin/dev` (post U-4)  
**Design:** [`object-schema-dsl.md`](../../../design/graph/object-schema-dsl.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Tighten the object-schema DSL and add field metadata used for identity and (later) search:

1. **Drop OBJECT-level `required: [...]`** from authoritative DSL / seeds / persisted `contentSchema`. Field-level `required` is the only authoring surface. Generated JSON Schema may still emit `"required"` arrays.
2. **SBOM ontology cleanup:** remove common `labels` / `attributes` fields from all seed schemas (and keep `SbomRegistry` + typed models + design in sync).
3. **`identifier: boolean`** on fields — scalar leaves (including under nested OBJECTs; not arrays). Project a flat identity `Map` (dotted paths). Values set on **create**; **immutable on update** (all write APIs + edit form).
4. **`searchable: boolean`** on fields — same placement rules; schema-editor checkbox; persist in DSL; **no** instance edit-form change; **no** search runtime in this story.

## Normative locks

| Topic | Lock |
|-------|------|
| OBJECT `required` list | Removed from model; ignored if present on read; never written |
| `identifier` / `searchable` | `BoMSchemaField` booleans, default `false` |
| Placement | Scalar types only (`STRING`/`NUMBER`/`INTEGER`/`BOOLEAN`/`ENUM`); nested under OBJECT OK; forbidden on ARRAY/OBJECT fields and under array `items` |
| Applies to | **Entity** and **edge-property** schemas (`ENTITY` / `EDGE_PROPERTIES`) — same flags and update immutability (G-1) |
| Identity map | Flat `Map<String, Any?>`, dotted paths; omit absent keys; skip arrays; empty map = no immutability constraint (G-7) |
| Update compare | Stored vs incoming projections (each with own schema); freeze only stored paths still marked `identifier` on incoming — new identifier paths may be set; downgrade dropping the flag is allowed (G-2 / G-15) |
| Update | Change/clear of a still-marked identity path → `IDENTIFIER_IMMUTABLE`; full payload replace (G-6) |
| Edit form | Identifier inputs read-only when id ∈ client `persistedIds` (loaded from server or saved this session) (G-3) |
| Searchable | Metadata only this story (G-11) |
| JSON Schema | `x-objs-identifier` / `x-objs-searchable` when true |

Detail and deferred items: [`GAPS.md`](GAPS.md).

## Out of scope

- Automatic pool dedupe / upsert-by-identity-map (G-12)
- Override-identity REST endpoint (G-12)
- Object search / FTS by searchable fields (G-11)
- Schema workbench chrome (U-4 deferral)
- Changing default of field `required` (stays `true`)
- Catalog migration when `identifier` set changes on existing types (G-8 accepted risk)

## Stages

| Stage | WIs | Ready |
|-------|-----|-------|
| 0 Docs | WI-001 | yes |
| 1 Required cleanup | WI-002 | after WI-001 |
| 2 Field flags DSL + editor | WI-003 | after WI-002 |
| 3 Identity projection | WI-004 | after WI-003 |
| 4 Immutable updates | WI-005 | after WI-004 |

## Work Items

- [x] WI-001 — Design + story trackers (`WI-001-design.md`)
- [x] WI-002 — Drop OBJECT-level `required` list (`WI-002-drop-object-required-list.md`)
- [x] WI-003 — `identifier` + `searchable` DSL and schema editor (`WI-003-field-flags-dsl.md`)
- [x] WI-004 — Identity map projection (`WI-004-identity-projection.md`)
- [x] WI-005 — Identifier immutability on update (`WI-005-identifier-immutable-update.md`)

## Acceptance (story)

- [x] Seeds / expert YAML have no OBJECT-level `required` lists; field-level `required` unchanged
- [x] SBOM ontology has no `labels` / `attributes` fields (seed + registry + typed models + design)
- [x] Schema editor exposes Identifier and Searchable checkboxes with normalizer rules
- [x] Update mutating identifier values fails with `IDENTIFIER_IMMUTABLE` on entity **and** edge-property write paths (G-1)
- [x] Edit form locks identifier fields for ids in `persistedIds` (G-3)
- [x] Searchable has no instance-form or search-API behavior
- [x] Design docs and tests green
