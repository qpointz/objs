# Story: Registry / graph multi-format I/O

**Slug:** `registry-graph-io-formats`  
**Branch:** `registry-graph-io-formats`  
**Status:** completed  
**Backlog:** C-7  
**Design:** [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md), [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md), [`docs/design/graph/object-schema-dsl.md`](../../../design/graph/object-schema-dsl.md)  
**Depends on:** [`schemas-catalog-overview`](../../completed/20260803-schemas-catalog-overview/STORY.md)

## Goal

Replace unified `/api/v1/objs/seeds/*` with **separated**, **format-extensible** import/export:

- **Registry (ontology)** — schemas + allow-list edges under `/api/v1/objs/registry/import|export`
- **Graph (instance content)** — entities + edges under `/api/v1/objs/graph/import|export`

Support `format=seeds` now; add **full-catalog JSON Schema** export on registry (`format=json-schema`) for object-model codegen.

## Confirmed decisions

| Topic | Choice |
|-------|--------|
| Separation | Registry I/O never accepts/emits `Graph`; graph I/O never accepts/emits catalog kinds |
| Old paths | Remove `/api/v1/objs/seeds/**` (no redirect) |
| Format selection | Required query param `format` (`seeds`; registry export also `json-schema`) |
| Seeds semantics | MERGE-only YAML via existing importer/serializer |
| Graph export | Annotation filter required; never unbounded dump |
| JSON Schema | Export only; latest ENTITY version per type (lexicographic max) |
| Edge → property | On source: camelCase(`role` + PascalCase(`targetType`)); `1:1` singular `$ref`; `1:*`/`UNSPECIFIED` array; skip `*` rules |
| UI | Point catalog overview at registry `format=seeds`; no JSON Schema download button yet |

## Stages

| Stage | Work items | Exit condition |
|-------|------------|----------------|
| 0 — Scaffold | WI-000 | Story + backlog + milestone planned |
| 1 — Registry seeds | WI-001 | Catalog import/export under registry with `format=seeds` |
| 2 — Graph seeds | WI-002 | Graph import/export; `/seeds/**` removed |
| 3 — JSON Schema | WI-003 | Full-catalog JSON Schema exporter + registry `format=json-schema` |
| 4 — UI + docs | WI-004 | UI paths + design/OpenAPI docs |

## Work Items

- [x] WI-000 — Story scaffolding (`WI-000-story-scaffold.md`)
- [x] WI-001 — Registry seeds import/export (`WI-001-registry-seeds-io.md`)
- [x] WI-002 — Graph seeds import/export; remove `/seeds/**` (`WI-002-graph-seeds-io.md`)
- [x] WI-003 — Full-catalog JSON Schema export (`WI-003-full-catalog-json-schema.md`)
- [x] WI-004 — UI paths + design docs (`WI-004-ui-docs.md`)

## Scope

- Multi-format registry and graph I/O endpoints
- Kind-scoped seed import
- Full-catalog JSON Schema (2020-12) exporter in `objs-core`
- UI catalog seed URL update
- Design + OpenAPI group updates

## Out of scope

- JSON Schema import / round-trip
- UI JSON Schema download button
- Semver-aware “latest” beyond lexicographic max
- Auth
- Persist-time cardinality enforcement
