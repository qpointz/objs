# Gaps — graph-mutate-replace (C-22)

Status: `open` | `resolved` | `deferred` | `cancelled`.

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-1 | Mode wire shape | **resolved** | **Kotlin:** `BoMMutateMode` enum on `BoMGraphMutation` (default `MERGE`). **REST:** verb selects semantic — `PUT /graphs/{id}` = **REPLACE**, `PATCH /graphs/{id}` = **MERGE**. Controller sets mode from verb (body must not disagree, or omit mode on wire). Validate: `PUT …/validate` → REPLACE dry-run; `PATCH …/validate` → MERGE dry-run. **Breaking:** Composer/Linter today use `PUT` for merge → switch Save to **PATCH**. |
| G-2 | REPLACE + explicit `unset` in same body | **resolved** | **Reject** any non-empty `entities.unset` / `edges.unset` under REPLACE (`REPLACE_UNSET_NOT_ALLOWED`). Desired set is `*.set` only. Composer Overwrite clears `unset` before PUT. |
| G-3 | Empty REPLACE `set` | **resolved** | **Allowed.** Empty `entities.set` + `edges.set` clears membership and graph-local edges; header/`graphId` kept. Accidental wipe mitigated by Composer Overwrite confirm. |
| G-4 | New entities without ids under REPLACE | **resolved** | **Same as MERGE** — allocate ids via persist gate / `prepareIds`. |
| G-5 | Composer / Linter UI for REPLACE | **resolved** | Composer Save exposes **Merge** (PATCH) vs **Overwrite** (PUT / REPLACE). Default **Merge**. Confirm when Overwrite (destructive prune). Object Linter follows same if it shares Save. |
| G-6 | Seed Graph REPLACE | **deferred** | Out of v1; stay MERGE |
| G-7 | Naming vs `mergeGraph` / `BoMGraphSpec.replace` | **resolved** | Glossary (normative docs WI-006): see below. No rename in v1 — disambiguate in docs/OpenAPI only. |
| G-8 | Mutation body shape | **resolved** | **Kind-first** (not op-first `upsert`/`delete`): `entities` / `edges`, each with `set` (payloads) and `unset` (ids). Kotlin `bomMutation { }` + `setAll(graph)`. Drop `BoMGraphUpsert` / `BoMGraphDelete`. Breaking reshape in **WI-008** before REPLACE (WI-002). Named-graph `entities.unset` = detach; pool = hard-delete. |

### Glossary (G-7)

| Name | What it is | Not |
|------|------------|-----|
| **`BoMMutateMode.MERGE`** + `PATCH /graphs/{id}` | Patch **one** existing graph: `*.set` + explicit `*.unset`; omission keeps | Combining several graphs into a new one |
| **`BoMMutateMode.REPLACE`** + `PUT /graphs/{id}` | Overwrite **one** graph’s membership + edges from `*.set` (stable id) | Id-only membership swap; multi-graph union |
| **`BoMNamedGraphStore.replace(id, BoMGraphSpec)`** | Set membership/`edgeIds` by **existing ids only** (no payload `set`) | Payload REPLACE mutate |
| **`BoMNamedGraphStore.mergeGraph(sourceIds, …)`** | Create a **new** graph = union of sources (`GraphMergePolicy` for duplicate keys) | In-place MERGE mutate |
| Seed **MERGE** | Catalog/graph seed import: set by identity; omission never deletes | Mutate mode or `mergeGraph` |

Composer UI labels: **Merge** → PATCH/`MERGE`; **Overwrite** → PUT/`REPLACE` (avoid saying “replace membership” for `BoMGraphSpec.replace` in product chrome).

### Body sketch (G-8)

```json
{
  "entities": { "set": [ /* BoMEntity */ ], "unset": [ "uuid" ] },
  "edges":    { "set": [ /* BoMEdge */ ],   "unset": [ "uuid" ] }
}
```
