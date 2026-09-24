# Story: Graph entity/edge vocabulary alignment

**Slug:** `graph-entity-edge-vocab`  
**Branch:** `graph-entity-edge-vocab`  
**Status:** in-progress  
**Folder:** [`docs/workitems/in-progress/graph-entity-edge-vocab/`](.)  
**Backlog:** [C-40](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Depends on:** — (independent of C-20)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md) · [`docs/design/graph/database-model.md`](../../../design/graph/database-model.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)  
**Related backlog:** [C-41](../../BACKLOG.md) freeze-scoped edge history (**not** this story)

## Goal

Align graph vocabulary on **entity** / **edge** (not **member**) across freeze SQL and REST so naming matches the pool (`/entities`) and the already-consistent edge table pair.

| Surface | Today | Target |
|---------|-------|--------|
| Live M2M SQL | `objs_graph_entity` | unchanged |
| Live M2M Kotlin | `GraphMembership*` | `GraphEntities*` |
| Freeze entity pins | `objs_graph_version_member` | `objs_graph_version_entity` |
| Edge live / history / freeze pins | `objs_graph_edge` / `objs_graph_edge_version` / `objs_graph_version_edge` | **unchanged** (no rename) |
| Attach/detach | `POST/DELETE /graphs/{id}/members/{entityId}` | `…/entities/{entityId}` (GraphMutation-aligned) |
| Graph edges | mutate only | `POST/PUT/DELETE /graphs/{id}/edges…` (graph-scoped only; no pool `/edges` CRUD) |
| Apply freeze structure | `…/apply-membership` | `…/apply-structure` |

**Breaking (pre-1.0):** no `/members` or `apply-membership` aliases.

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | This folder + backlog row |
| 1 — Design lock | WI-001 | done | Paths, V9 rename map, Kotlin renames; GAPS closed |
| 2 — Persistence | WI-002 | done | Flyway V9 + `GraphEntities*` + `GraphVersionEntity*` |
| 3 — Entity REST | WI-003 | done | `/members` → `/entities` + `apply-structure` + workbench |
| 4 — Edge REST | WI-004 | done | Graph-scoped edge CRUD |
| 5 — Consumers | WI-007 | done | Review/adjust workbench + SBOM + AR |
| 6 — Docs prose | WI-005 | done | Living design prose / path tables |
| 7 — ER diagrams | WI-006 | done | Persistence mermaid ER |

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock: paths, V9 map, Kotlin renames — examples: **docs** (`WI-001-design-lock.md`)
- [x] WI-002 — Persistence: Flyway V9 + `GraphEntities*` + `GraphVersionEntity*` — examples: **—** (`WI-002-persistence-rename.md`)
- [x] WI-003 — REST: `/entities` attach/detach + `apply-structure` — examples: **workbench** (`WI-003-rest-entities.md`)
- [x] WI-004 — REST: `/graphs/{id}/edges` CRUD — examples: **—** (`WI-004-rest-edges.md`)
- [x] WI-007 — Consumer review: workbench + SBOM + AR — examples: **workbench + SBOM + AR** (`WI-007-consumers-review.md`)
- [x] WI-005 — Living docs (prose / paths) — examples: **docs** (`WI-005-living-docs.md`)
- [x] WI-006 — Persistence ER diagrams — examples: **docs** (`WI-006-persistence-er.md`)

## Out of scope

- Renaming table `objs_graph_entity` (SQL already entity-aligned; Kotlin → `GraphEntities*`)
- Renaming `objs_graph_edge_version` / `objs_graph_version_edge` (leave as-is; `objs_edge_version` rejected)
- Freeze-scoped edge history redesign ([**C-41**](../../BACKLOG.md) backlog only)
- Pool-style `/api/v1/objs/edges` CRUD mirroring `/entities/*` (edges only under `/graphs/{id}/edges…`)
- Soft aliases for old paths
- Top-level `/edges/{id}` history/compact controller changes
- Forcing examples onto new edge REST (persistence mutate OK; WI-007 reviews compile/copy only)
- Store text search (C-20)
- Story closure / archive (user must ask)

## Acceptance (after implementation)

- [x] Freeze entity pin table/index pairs with live `objs_graph_entity` the same way edges already pair
- [x] No production references to `/members`, `objs_graph_version_member`, `GraphMembership`, or `GraphVersionMember`
- [x] Graph edge single-item CRUD under `/graphs/{id}/edges…` only (no pool `/edges` CRUD)
- [x] Persistence ER diagrams show `objs_graph_version_entity` (WI-006)
- [x] Workbench + SBOM + AR reviewed/adjusted (WI-007)
- [x] `./gradlew :objs-persistence:test :objs-autoconfigure:test :objs-service:test :objs-service-ui:build :sbom-service:test :asset-repository-service:test`

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI.  
2. Do not start WI-002 until WI-001 checkboxes are done.  
3. Do not close this story until the user asks.
