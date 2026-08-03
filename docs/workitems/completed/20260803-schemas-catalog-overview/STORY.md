# Story: Schemas catalog overview

**Slug:** `schemas-catalog-overview`  
**Branch:** `schemas-catalog-overview`  
**Status:** completed  
**Backlog:** U-2  
**Design:** [`docs/design/ui.md`](../../../design/ui.md), [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md)  
**Depends on:** [`schema-workbench-unify`](../20260803-schema-workbench-unify/STORY.md)

## Goal

Add a Schemas **entry-point overview** that shows the **full catalog**: all ENTITY object types and
allowed-edge relations, with quick navigation into the existing per-type workbench, plus
**export / import** of catalog definitions via seed APIs.

## Confirmed decisions

| Topic | Choice |
|-------|--------|
| Landing | `/schemas` (no type) shows overview; type/version routes keep per-type detail |
| Primary canvas | Ontology graph: ENTITY types as nodes, allow-list rules as edges |
| Edge-property schemas | Stay in type list (E); not graph nodes; edge labels show role · cardinality |
| Overview layout | Dagre LR orthogonal (`step`) edges; no selection inspector pane |
| Wildcards | Single `*` node for `*` endpoints |
| Import/export | Catalog-only seeds (`GET /seeds/export`, `POST /seeds/import`); reject files containing `Graph` |
| Import semantics | MERGE only (no deletes); refresh overview after success |
| Edge editing on overview | Out of scope — edit remains on type detail |

## Stages

| Stage | Work items | Exit condition |
|-------|------------|----------------|
| 0 — Shell | WI-000 | Overview landing + back-to-overview from detail |
| 1 — Ontology graph | WI-001 | Full types+edges graph; click navigates |
| 2 — Catalog I/O | WI-002 | Export/import toolbar via seeds |
| 3 — Docs | WI-003 | ui.md + seeds note + smoke tests |

## Work Items

- [x] WI-000 — Overview shell as Schemas landing (`WI-000-overview-shell.md`)
- [x] WI-001 — Ontology graph all types + edges (`WI-001-ontology-graph.md`)
- [x] WI-002 — Catalog seed export / import (`WI-002-catalog-seed-io.md`)
- [x] WI-003 — Docs + smoke tests (`WI-003-docs-tests.md`)

## Scope

- Schemas overview panel + ontology React Flow graph
- `listEdges` + seed export/import clients in UI `api.ts`
- Docs and vitest for graph builder / import filter

## Out of scope

- Persist-time cardinality enforcement
- Editing allow-list edges on the overview canvas
- Graph instance explorer changes
- Schemas-only / edges-only seed HTTP flags
- Deleting catalog entries via import
- Auth
