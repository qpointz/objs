# Gaps — live-store-apis (C-17)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

Locks below are the story contract. Living design is updated in WI-001.

---

## Architecture

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-A1 | Where APIs live | **resolved** | `:objs-core` programmatic (Java-friendly). Workbench may wrap; examples use core, not `/api/v1/objs/**` |
| G-A2 | `copyGraph` vs `clone` | **resolved** | `copyGraph` = one source → new **live** graph, same entity ids, copied graph-local edges. **Keep-split new draft** + **AR collection copy**. `clone()` unchanged this story. **Fingerprint/freeze = C-18 pins**, not `copyGraph` or `mergeGraph` |
| G-A3 | Latest schema version | **resolved** | One core comparer for catalog “latest”: numeric SemVer-ish tuples (`1.2.0` > `1.10.0` is **false**). Do **not** use lexicographic string max. Product app versions stay in SBOM (`SemVerVersionComparer`) |
| G-A4 | Identity query storage | **resolved** | No new identity column this story. Query uses existing payload JSON + `BoMIdentityProjection`. Store may scan-by-type then group; must not leave grouping in example services |
| G-A5 | Reverse lookup storage | **resolved** | Use existing `bom_graph_entity(entity_id)` index. No new table. `listIncidentEdges` uses graph-local `bom_graph_edge` (`source_id` / `target_id`); optional `graphId` filter |
| G-A6 | Paging | **resolved** | 1-based `page`, `size` in `1..100` (default 20). Stable order: `type`, `id`. Total count returned. No matcher-DSL pagination keys |
| G-A7 | `countByType` | **resolved** | Pool-wide map `type → Long`. Optional graph-scoped overload counting **members** of that graph. No “used in applications” |
| G-A8 | Allow-list for type | **resolved** | Inbound = `targetType` is type or `*`. Outbound = `sourceType` is type or `*`. Same as workbench `edgesForType`. Core helper; workbench + examples call it |
| G-A9 | Field hints | **resolved** | Walk OBJECT fields (not ARRAY). Return dotted paths with `identifier` / `searchable` flags + display title (field title unless generic scalar title). First-level scalars optional helper for grids |
| G-A10 | Display label | **resolved** | `payload["name"]` if non-blank; else first identifier path value; else `type`. SBOM may still append `@version` in the **product** DTO |
| G-A11 | Filter map → `obj-expr` | **resolved** | Equality only, `&&` joined. Keys are searchable paths (or `type` / `id`). Escape string literals. Apps may still pass a raw `obj-expr` |
| G-A12 | Identifier immutability | **resolved** | Persist gate already enforces. Remove SBOM `AssetInventoryService.update` duplicate check |
| G-A13 | Empty identity | **resolved** | Entities with empty projected identity are omitted from duplicate groups and identity find |
| G-A14 | Incident edge shape | **resolved** | Return store `BoMEdge` (includes `graphId`). Product labels (`DEPENDS_ON` → “Depends on”) stay in examples |
| G-A15 | Catalog “used in” | **resolved** | **Domain.** Core does not map graph ids → applications/collections. Examples keep that join |
| G-A16 | Example rewiring | **resolved** | Feature WIs rewire **every labeled consumer** in the same commit (SBOM, AR, workbench when labeled). Maps: [`EXAMPLES.md`](EXAMPLES.md), [`WORKBENCH.md`](WORKBENCH.md) |
| G-A17 | Docs cadence | **resolved** | Document the API in the **same** feature WI (`apps-vs-foundation` + product `example.md` / README when the domain contract changes + workbench help if Objects/query UX changes + core KDoc). WI-007 is a sweep, not the first write-up |
| G-A18 | AR `copyGraph` | **resolved** | **Live collection copy** (shared object ids): `copyGraph` + new `ar_collection` row. Not a freeze snapshot |
| G-A19 | Text `q` / contains | **deferred** | **C-20** [`store-text-search`](../../planned/store-text-search/STORY.md). Not designed in this story |
| G-A20 | Store audit timestamps | **deferred** | **C-19** after versions: clocks on **version** rows, not in-place entity/edge columns |
| G-A21 | `mergeGraph` vs `copyGraph` | **resolved** | Separate API. `mergeGraph(sourceIds, annotations, GraphMergePolicy)` persist-unions 1..n live graphs. Default **`FirstSeenGraphMergePolicy`**: node key = entity id; edge key = `(source, role, target)`; keep first; no property-map merge. Empty ids → `GRAPH_MERGE_EMPTY`; any missing source → `GRAPH_NOT_FOUND`, no new graph. **Combine-on-new-draft** only. Combined SBOM GET stays ephemeral `BomUnion`. Do not overload `copyGraph` with a collection |

---

## Open

_(none — remaining work is implementation)_

---

## Out of story

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | FB-3 contains/`q` | **deferred** | **C-20** [`store-text-search`](../../planned/store-text-search/STORY.md) |
| G-X11 | FB-3 operators other than contains/`q` | **deferred** | **C-19** after C-20 |
| G-X2 | D-6 transactional inventory Save | **deferred** | Product backlog |
| G-X3 | D-7 file demo inventory | **deferred** | Product backlog |
| G-X4 | AR `implementation` on `:objs-service` | **deferred** | Sidecar compile leak (`SpaRoutingFilter`). Platform/boot follow-up, not store APIs |
| G-X5 | Shared example UI kit | **cancelled** | Copy-both-UIs stays |
| G-X6 | Change `clone()` to membership copy | **cancelled** | Keep hard clone; add `copyGraph` and `mergeGraph` |
| G-X7 | Identity unique index / merge | **cancelled** | Find-only duplicates (G-P7) |
| G-X8 | `created_by` / audit log / membership-row clocks | **cancelled** | |
| G-X9 | Versions + snapshot pins | **deferred** | **C-18** [`versions-and-snapshots`](../../planned/versions-and-snapshots/STORY.md) |
| G-X10 | Entity/edge/graph store clocks; pin reverse lookup; FB-3 remainder | **deferred** | **C-19** [`foundation-after-versions`](../../planned/foundation-after-versions/STORY.md) |
