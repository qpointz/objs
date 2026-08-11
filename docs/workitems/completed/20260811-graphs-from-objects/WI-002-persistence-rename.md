# WI-002 — Flyway renames + edge `graph_id`

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design + DB + store  
**Status:** done  
**Depends on:** WI-001  
**Modules:** `:objs-core` (migrations + JPA)

## Goal

Apply STORY **table rename map**. Edges become graph-local. Drop `bom_subgraph_edges`.

## Algorithm

```text
1. RENAME bom_graph_entity        → bom_entity
2. RENAME bom_graph_entity_schema → bom_entity_schema
3. RENAME bom_graph_edge_schema   → bom_edge_schema
4. RENAME bom_subgraph            → bom_graph
5. RENAME bom_subgraph_entities   → bom_graph_entity
   RENAME COLUMN subgraph_id      → graph_id
6. ADD bom_graph_edge.graph_id (NULL → backfill from bom_subgraph_edges → NOT NULL + FK CASCADE)
7. DROP bom_subgraph_edges
8. Indexes: entity(type,version), edge(graph_id/source/target/role), membership(entity_id)
```

**Do not** add `parent_graph_id` or `kind` on `bom_graph`.

## JPA

`@Table` / columns for all renames; membership `(graph_id, entity_id)`; edge requires `graphId`.

## Tests

| Case | Expect |
|------|--------|
| Empty DB migrate | Matches STORY ER |
| Backfill from C-12 | Membership + edge ownership survive |
| Multi-graph edge in old M2M | Detect/fail or document (should not occur) |

## Out of scope

- Store/matcher behaviour (WI-003)

## Acceptance

- [x] Migration applies (H2 + Postgres path as project requires)
- [x] No `bom_subgraph*` tables; no lineage columns on `bom_graph`
- [x] JPA compiles against new names
- [x] STORY `[x]`; commit; push

## Implementation notes

- `BoMAllowedEdgeRuleRecord` → `@Table("bom_edge_schema")`.
- `BoMSubgraphEntityRepository`: `findByGraphId` / `deleteByGraphId` / `countByGraphId`;
  `BoMSubgraphEdgeRepository` removed (edges own `graph_id` directly).
- `BoMEdgeRepository`: added `findByGraphId` / `countByGraphId`.
- `BoMEdge` domain gained optional `graphId: UUID?` for persist round-trip; `BoMGraphStore`
  upsert requires a `graphId` (from the edge or the existing row) — edges can never exist
  without an owning graph (`graph_id` NOT NULL FK).
- `BoMSubgraphStore`: `resolve()` reads entities via M2M membership and edges via
  `edgeRepository.findByGraphId`; `replaceMembership` writes entity M2M rows and
  (re)assigns/removes edges by `graph_id`; `snapshot()` creates the new graph header
  before writing cloned edges (FK requires the graph to exist first), then adds entity
  membership only (edges already own `graph_id`).
- `BoMRawGraphReader` SQL: pool table renamed `bom_graph_entity` → `bom_entity`;
  `bom_graph_edge` unchanged.
- Updated `objs-core` persistence tests (`BoMSubgraphPersistenceTest`,
  `BoMSubgraphStoreTest`, `BoMSubgraphMatcherSelectTest`, `BoMGraphStoreTest`,
  `BoMGraphStorePostgresIT`) to create/attach a graph before writing edges.
- `:objs-core:test` green (127/127); `V4` migration untouched.

## Commit message hint

`[feat] Rename tables to bom_entity/bom_graph; edge graph_id (WI-002)`
