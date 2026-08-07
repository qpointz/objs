# WI-002 — Domain store: CRUD + resolve

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Persistence + domain API  
**Status:** done  
**Depends on:** WI-001  
**Modules:** `:objs-core`

## Goal

Domain API to create/update/delete soft-link subgraphs and **get-by-id / resolve** to `BoMSubgraph` with original ids and **latest** payloads (G-S2, G-S12, G-S7 programmatic API). `get(id)` is the non-expression open path for app-held `subgraphId` refs.

## Suggested API shape

Place beside or inside `BoMGraphStore` (prefer a dedicated `BoMSubgraphStore` / `BoMNamedSubgraphService` if `BoMGraphStore` is already large — either is fine if Spring-wired and tested).

```kotlin
data class BoMSubgraphHeader(
    val id: UUID,
    val annotations: Map<String, String>,
)

data class BoMSubgraphSpec(
    val id: UUID? = null,
    val annotations: Map<String, String> = emptyMap(),
    val entityIds: Set<UUID> = emptySet(),
    val edgeIds: Set<UUID> = emptySet(),
)

data class BoMResolvedSubgraph(
    val id: UUID,
    val annotations: Map<String, String>,
    val subgraph: BoMSubgraph, // entities + edges, live
)

interface /* or class */ {
    fun create(spec: BoMSubgraphSpec): BoMResolvedSubgraph
    fun replace(id: UUID, spec: BoMSubgraphSpec): BoMResolvedSubgraph  // full replace annotations+membership
    fun delete(id: UUID)
    fun get(id: UUID): BoMResolvedSubgraph?
    fun list(): List</* id, annotations, entityCount, edgeCount */>
}
```

Names can vary; behaviour is normative.

## Validation rules (must implement)

1. Every `entityId` exists in `bom_graph_entity` (else error).
2. Every `edgeId` exists in `bom_graph_edge` (else error).
3. For each member edge, `source` and `target` are both in `entityIds` (G-S2).
4. `annotations` non-null (empty map allowed).
5. `replace` / `create` with client-supplied id: conflict if id already exists on create; 404 semantics on replace missing id.

## Resolve behaviour

1. Load membership id sets.
2. Load full `BoMEntity` / `BoMEdge` domain objects by those ids (reuse existing record→domain mapping in store/reader).
3. Return lists; **do not** remap ids; **do not** re-induce edges (only stored edge members).
4. After an entity payload update in the main graph, resolve must show the new payload without touching membership (test G-S12).

## Tests (required)

| Case | Expect |
|------|--------|
| Round-trip create → get | Same entity/edge UUIDs; payloads match store |
| Overlay update entity payload | Resolve sees new payload; membership unchanged |
| Edge without endpoint member | Reject |
| Missing entity/edge id | Reject |
| Delete subgraph | Objects remain; get → empty/missing |
| Delete entity | Membership row gone from all subgraphs |

## Out of scope

- HTTP (WI-003)
- Matcher (WI-004)
- Snapshot clone (WI-007) — may leave a package hook/`TODO` only if needed; prefer implement snapshot in WI-007
- UI

## Implementation checklist

- [x] Service + validation
- [x] Unit tests above
- [x] STORY tracker `[x]`; commit; push

## Acceptance

- [x] Round-trip preserves entity and edge UUIDs
- [x] Annotations round-trip on header
- [x] Invalid edge membership rejected
- [x] Delete subgraph leaves graph objects intact
- [x] Live resolve verified by mutate-then-resolve test

## Commit message hint

`[feat] Add soft-link subgraph store API (WI-002)`
