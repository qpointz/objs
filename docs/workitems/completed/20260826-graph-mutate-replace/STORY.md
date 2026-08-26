# Story: Graph mutate MERGE vs REPLACE

**Slug:** `graph-mutate-replace`  
**Branch:** `graph-mutate-replace`  
**Status:** completed  
**Closed:** 2026-08-26  
**Folder:** [`docs/workitems/completed/20260826-graph-mutate-replace/`](.)  
**Backlog:** [C-22](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Design:** [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md), [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Add an explicit **MERGE vs REPLACE** semantic for **named-graph** mutation so callers can periodically
rebuild graph contents on a **stable `graphId`** (analytics “uber graph”, duplicate-resolved
aggregates) while keeping deep-version history via `createDeepGraphVersion`.

Today `BoMNamedGraphStore.mutate` / `PUT /graphs/{id}` is **MERGE/patch only** (omission never
deletes). Delete + recreate breaks stable id and version lineage. Client-computed prune is fragile.

Also reshape the mutation body from op-first `upsert`/`delete` to kind-first `entities`/`edges` ×
`set`/`unset` (G-8 / WI-008) before adding REPLACE.

## Driver use case

1. Long-lived graph id (e.g. analytics uber-graph across SBOM apps).
2. Each processing run: **REPLACE** membership + graph-local edges with newly resolved contents.
3. Optionally `createDeepGraphVersion(graphId, …)` so each run is pinned history on the **same** id.
4. HEAD is overwritten; pool entities shared with app BOMs stay (detach, don’t pool-delete).

## Normative (locked — WI-001 + G-8)

| Topic | Lock |
|-------|------|
| Default | **MERGE** — `BoMMutateMode.MERGE` default; omission never deletes |
| Body shape | Kind-first: `entities` / `edges`, each `{ set, unset }` (G-8); drop `upsert`/`delete` |
| Kotlin builder | `bomMutation { entities { set / unset }; edges { … } }` + `setAll(graph)` |
| REPLACE | `*.set` is the full desired membership + graph-local edges; not listed → detach / drop edge |
| REPLACE + `unset` | Non-empty `*.unset` → **reject** (`REPLACE_UNSET_NOT_ALLOWED`) |
| Empty REPLACE | **Allowed** — empty both `set` clears members + edges; `graphId` / header kept |
| Missing ids | Allocate like MERGE (`prepareIds`) |
| Scope | Named graph only — not pool `BoMGraphStore.mutate`; not pool wipe |
| Entity unset | Named graph: **detach** (+ incident edges); pool entity kept. Pool mutate: hard-delete |
| Validation | One TX; validate **final** projection |
| Versions | REPLACE updates **HEAD**; snapshot via `createDeepGraphVersion` remains explicit |
| Seeds | **MERGE-only** in v1 (G-6 deferred) |
| `replace(BoMGraphSpec)` | Unchanged id-set membership API; not payload REPLACE |
| Kotlin mode | `BoMMutateMode` on `BoMGraphMutation` |
| REST | **`PATCH` = MERGE**, **`PUT` = REPLACE** (verb sets mode; omit mode on wire or must agree) |
| Validate | `PATCH …/validate` MERGE; `PUT …/validate` REPLACE |
| Composer | **Merge** (PATCH, default) vs **Overwrite** (PUT + confirm); Overwrite sends set-only |
| Naming | See [`GAPS.md`](GAPS.md) glossary (G-7) |

### Programmatic API (`:objs-core`)

```text
bomMutation {
  entities { set(...); unset(...) }
  edges { set(...); unset(...) }
}
// or: bomMutation { setAll(graph) }

mutation.mode = BoMMutateMode.MERGE | REPLACE   // default MERGE; REST sets from verb
namedGraphs.mutate(graphId, mutation)
namedGraphs.validateMutate(graphId, mutation)
```

### REST body

```json
{
  "entities": { "set": [], "unset": [] },
  "edges": { "set": [], "unset": [] }
}
```

### REST (`:objs-service`)

| Endpoint | Semantic |
|----------|----------|
| `PATCH /api/v1/objs/graphs/{id}` | MERGE |
| `PUT /api/v1/objs/graphs/{id}` | REPLACE |
| `PATCH /api/v1/objs/graphs/{id}/validate` | MERGE dry-run |
| `PUT /api/v1/objs/graphs/{id}/validate` | REPLACE dry-run |

**Migration:** workbench Save merge path: today’s `PUT` → **`PATCH`**; body → kind-first (WI-008).

### Consumers

| Surface | Work |
|---------|------|
| All mutate call sites | Kind-first body + builder (WI-008) |
| Composer / Object Linter | Merge vs Overwrite Save (WI-005) |
| SBOM `replaceBom` | Rewire to core REPLACE (WI-004) |
| SBOM uber-graph / analytics | Document pattern; product feature out of scope |
| Asset repository | Audit exact-set sync; change only if needed (WI-004) |
| Inventory OpenAPI | Legacy `example-sbom` removed (WI-007 done) |

### Affected components

| Component | Impact |
|-----------|--------|
| `BoMGraphMutation` | WI-008: kind-first shape; WI-002: `mode` enum; drop Upsert/Delete |
| Builder | `bomMutation` / `setAll` (WI-008) |
| `BoMNamedGraphStore.mutate` / `validateMutate` | WI-008 fields; WI-002 REPLACE prune |
| `BoMGraphStore.mutate` (pool) | Field migrate only (MERGE); REPLACE out of scope |
| `ObjsGraphsController` | Body shape; then PATCH merge / PUT replace |
| Workbench `api.ts` / Composer | New JSON; then PATCH vs PUT Save |
| `GraphSeedHandler` | New shape; stay MERGE |
| Tests | Core, REST, SBOM, Composer |
| Gremlin / matchers / `copyGraph` / `mergeGraph` | Unaffected (naming only) |
| D-6 transactional Save | Later consumer; don’t block |

## Stages

| Stage | WIs | Status | Notes |
|-------|-----|--------|-------|
| 0 — Scaffold | WI-000 | done | |
| 0b — SBOM OpenAPI cleanup | WI-007 | done | Inventory-only OpenAPI |
| 1 — Design lock | WI-001 | done | Modes + GAPS; G-8 locked in docs |
| 1b — Body reshape | WI-008 | done | Kind-first + builder; PUT still MERGE |
| 2 — Core REPLACE | WI-002 | done | `BoMMutateMode` + prune; Kotlin API |
| 3 — Driver example | WI-004 | done | `replaceBom` dogfoods REPLACE (no REST needed) |
| 4 — REST verbs | WI-003 | done | PATCH=MERGE / PUT=REPLACE |
| 5 — Workbench | WI-005 | done | Merge vs Overwrite Save |
| 6 — Docs | WI-006 | done | Living design + glossary |

```text
WI-008 → WI-002 → WI-004 (driver)
                 ↘ WI-003 → WI-005
                              ↘ WI-006 (also after 004)
```

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-007 — Remove legacy `example-sbom` REST/OpenAPI; keep inventory only — examples: **SBOM** (`WI-007-sbom-openapi-cleanup.md`)
- [x] WI-001 — Design lock — examples: **docs** (`WI-001-design-lock.md`)
- [x] WI-008 — Mutation body reshape + builder — examples: **—** (`WI-008-mutation-reshape.md`)
- [x] WI-002 — Core REPLACE mutate + tests — examples: **—** (`WI-002-core-replace.md`)
- [x] WI-004 — SBOM `replaceBom` rewire + AR audit — examples: **SBOM + AR** (`WI-004-examples.md`)
- [x] WI-003 — REST PATCH/PUT + validate — examples: **—** (`WI-003-rest.md`)
- [x] WI-005 — Composer Merge vs Overwrite Save — examples: **workbench** (`WI-005-workbench.md`)
- [x] WI-006 — Living docs — examples: **docs** (`WI-006-living-docs.md`)

## Out of scope

- Seed document `REPLACE` mode
- Pool-level REPLACE / hard-delete of pool entities on prune
- Renaming `mergeGraph` / `replace(BoMGraphSpec)`
- Changing Gremlin
- Distributed multi-replica coordination
- Implementing the analytics uber-graph product feature itself (API enabler only)

## Acceptance

- [x] Mutation body is kind-first `set`/`unset`; builder is the Kotlin path
- [x] Same `graphId` rebuilt twice with different contents → second run matches payload exactly
- [x] Empty REPLACE clears members/edges; id kept
- [x] Non-empty `unset` under REPLACE → 400
- [x] `createDeepGraphVersion` still works after REPLACE
- [x] Composer default Save = Merge (PATCH); Overwrite = PUT + confirm
- [x] Seeds / MERGE callers unchanged in behaviour (after reshape migrate)
- [x] `./gradlew :objs-core:test :objs-service:test :sbom-service:test :asset-repository-service:test`
