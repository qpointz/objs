# WI-003 — Subgraph selection by annotations

**Story:** [`STORY.md`](STORY.md)  
**Status:** pending  
**Depends on:** WI-002  
**Gaps:** G-2 (match-all), G-3 (induced source/target), G-4 (`BoEntity` / `BoEdge`)

## Goal

Implement **subgraph** selection: match entities via an annotation **matcher**, then **add edges**
that exist on the selected entities — per
[`docs/design/graph/annotations-and-subgraphs.md`](../../../design/graph/annotations-and-subgraphs.md).

## Scope

- **Matcher extension point** — base class / interface for annotation matching strategies
- **Default strategy: match-all** — filter is a key-value map; entity matches when **all** filter
  entries are present on the entity (extra entity annotations OK)
- Subgraph API: apply matcher → entity subset → **induced** edges (source and target both in set)
- Edge ends named **source** / **target**
- Edges remain **unannotated** (provisional)
- In-memory only; unit tests

## Out of scope

- Additional matcher strategies beyond match-all (extension point only)
- Persistence-backed subgraph query (may reuse API later in WI-005)
- REST
- Edge annotations

## Acceptance

- [ ] Base matcher type exists; match-all implementation behaves as specified
- [ ] Given a graph and match-all filter map, returns expected entity subset and induced edges
- [ ] Edge with only source (or only target) in the set is **excluded**
- [ ] Tests: full match, partial miss, extra annotations on entity still match; induced edge cases
