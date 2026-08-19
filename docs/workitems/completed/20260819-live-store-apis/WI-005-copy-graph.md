# WI-005 — `copyGraph` + `mergeGraph`

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Membership copy / merge  
**Status:** complete  
**Depends on:** WI-001  
**Examples:** **SBOM + AR**

## Goal

Two **separate** live-graph APIs on `BoMNamedGraphStore`. Neither changes `clone()`. Neither is fingerprint/freeze (C-18 pins).

| API | Sources | Collision policy | Use |
|-----|---------|------------------|-----|
| `copyGraph(sourceId, annotations)` | exactly one | none | SBOM **keep-split** new draft; AR **collection copy** |
| `mergeGraph(sourceIds, annotations, policy)` | 1..n | `GraphMergePolicy` | SBOM **combine-on-new-draft** |

Do **not** overload `copyGraph` with a collection of ids. Combined SBOM **GET** stays ephemeral `BomUnion` — do **not** persist it.

Internally the two methods may share a private persist helper (new graph header + membership + new edge rows). Public signatures stay distinct. `copyGraph` must **not** insert new pool entities (unlike `clone()`).

## Core

- [x] `copyGraph(sourceId, annotations) → BoMResolvedGraph` (G-A2, G-A18)
  - New graph id + header annotations
  - Same pool entity ids (membership only)
  - Graph-local edges copied with **new edge ids** and new `graphId`
  - Missing source → same `GRAPH_NOT_FOUND` shape as `clone`
- [x] `mergeGraph(sourceIds, annotations, policy) → BoMResolvedGraph` (G-A21)
- [x] Overload `mergeGraph(sourceIds, annotations)` → `FirstSeenGraphMergePolicy`
- [x] `GraphMergePolicy` (not `DuplicateStrategy` — that word is FB-2 identity twins):
  - `nodeKey(entity)` / `edgeKey(edge)` — detect collision
  - `onDuplicateNode(kept, incoming)` / `onDuplicateEdge(kept, incoming)` — resolve
- [x] Default `FirstSeenGraphMergePolicy`: node key = entity id; edge key = `(source, role, target)`; keep first in caller order; do not merge property maps
- [x] Empty `sourceIds` → `GRAPH_MERGE_EMPTY`; any missing source → `GRAPH_NOT_FOUND` and **no** new graph
- [x] Tests: copy members/edges equal by entity id; pool size unchanged; `clone` still new entity ids; merge overlaps first-seen; custom policy prefers incoming; empty/missing sources; n=1 merge membership matches copy

## SBOM

- [x] Keep-split new draft + fingerprint-based draft → `namedGraphs.copyGraph` (shared live objects)
- [x] Combine-on-new-draft → `namedGraphs.mergeGraph(sourceBomGraphIds, annotations)` (not `BomUnion` + app `materialize`)
- [x] Combined SBOM GET / multi-select / MI / CDX stay **`BomUnion`** (ephemeral)
- [x] **Do not** rewire fingerprint freeze to copy or merge
- [x] Drop app-local `copyGraph` / `materialize` helpers once unused
- [x] Tests in `:sbom-service`

## AR

- [x] **Collection copy** (new this story): domain `POST` (or equivalent) clones collection metadata + `copyGraph` → new `graph_id` on the new `ar_collection` row (accepted types, write mode, owner fields copied)
- [x] `mergeGraph` has **no AR consumer** this WI (core tests only)
- [x] Tests in `:asset-repository-service`

## Docs (same commit)

- [x] `apps-vs-foundation.md` — `copyGraph` and `mergeGraph` **shipped**; `clone` remains hard snapshot
- [x] Graph persistence/model: **live** copy = `copyGraph`; **live** union = `mergeGraph`; snapshot/fingerprint ≠ this WI
- [x] `docs/design/sbom/example.md` — keep-split `copyGraph`; combine-on-new-draft `mergeGraph`; Combined GET = `BomUnion`
- [x] `docs/design/asset-repository/example.md` + `examples/asset-repository/README.md` — collection copy route
- [x] KDoc

## Out of scope

- Changing Composer `clone` UX; deep-copying pool payloads into new entity ids
- Workbench REST for copy/merge (Composer keeps `clone()`)
- Persisting Combined SBOM GET
- Fingerprint / freeze (C-18)

## Acceptance

- SBOM **keep-split new draft** shares pool ids with each source graph (fingerprint unchanged this WI)
- SBOM **combine-on-new-draft** persists one new graph via `mergeGraph` (first-seen)
- Combined SBOM GET still does not persist
- AR collection copy shares pool ids; two collection rows; distinct `graph_id`s
- `clone()` tests still pass
- `./gradlew :objs-core:test :sbom-service:test :asset-repository-service:test`
