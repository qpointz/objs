# Story: JSON Schema export options

**Slug:** `json-schema-generation`  
**Branch:** `json-schema-generation`  
**Status:** completed  
**Backlog:** C-10  
**Design:** [`docs/design/graph/object-schema-dsl.md`](../../../design/graph/object-schema-dsl.md), [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md), [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md), [`docs/design/ui.md`](../../../design/ui.md)  
**Depends on:** [`registry-graph-io-formats`](../../completed/20260805-registry-graph-io-formats/STORY.md)

## Goal

Make full-catalog JSON Schema export **configurable** for codegen: a `BoMJsonSchemaExportOptions` object (dialect, edge inclusion, edge-property `$defs`) shared by the programmatic exporter and `GET /api/v1/objs/registry/export?format=json-schema`. Support **linked** edges so relation props appear on both ends (e.g. Database `containsDataset` collection and Dataset `containsFromDatabase` parent).

## Confirmed decisions

| Topic | Choice |
|-------|--------|
| Options type | `BoMJsonSchemaExportOptions` in `objs-core` |
| Defaults | Match today’s export (`dialect=2020-12`, `includeEdges=outbound`, `includeEdgePropertySchemas=true`) |
| `includeEdges` | `none` \| `outbound` \| `linked` |
| Linked | Outbound on source + inverse on target; inverse name `camelCase(role + "From" + PascalCase(sourceType))` |
| Inverse cardinality | Outbound `1:*`/`UNSPECIFIED` → singular inverse; `1:1` → array inverse |
| REST | Flat query params on existing GET export when `format=json-schema` |
| UI | Shared options on Schemas overview Text + Export |
| Per-type JSON Schema tab | Unchanged (payload-only) |

## Stages

| Stage | Work items | Exit condition |
|-------|------------|----------------|
| 0 — Scaffold | WI-000 | Story + backlog + milestone |
| 1 — Core options | WI-001 | Exporter respects options; unit tests |
| 2 — REST | WI-002 | Query params + OpenAPI + controller tests |
| 3 — UI | WI-003 | Catalog overview controls + `api.ts` |
| 4 — Docs | WI-004 | Design docs updated |

## Work Items

- [x] WI-000 — Story scaffolding (`WI-000-story-scaffold.md`)
- [x] WI-001 — Export options + exporter (`WI-001-export-options.md`)
- [x] WI-002 — REST query params (`WI-002-rest-openapi.md`)
- [x] WI-003 — Schemas overview UI (`WI-003-ui-options.md`)
- [x] WI-004 — Design docs (`WI-004-docs.md`)

## Scope

- Options model + full-catalog exporter behavior
- Registry export query params
- Schemas overview Text/Export UI
- Design + OpenAPI updates

## Out of scope

- JSON Schema import / round-trip
- Dialects other than 2020-12
- Kotlin/TS codegen from the schema
- Semver-aware latest version selection
- Per-type `…/json-schema` options
