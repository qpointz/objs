# WI-003 — Subgraph selection by annotations

**Story:** [`STORY.md`](STORY.md)  
**Status:** pending  
**Depends on:** WI-002

## Goal

Implement **subgraph** selection: filter entities by annotations, then **add edges** that exist
on the selected entities — per
[`docs/design/graph/annotations-and-subgraphs.md`](../../../design/graph/annotations-and-subgraphs.md).

## Scope

- Annotation filter API (matching semantics documented; keep simple — e.g. all listed annotations present)
- Result type: subgraph = matched entities + additive edges
- Edges remain **unannotated** (provisional)
- In-memory only; unit tests

## Out of scope

- Persistence-backed subgraph query (may reuse API later in WI-005)
- REST
- Edge annotations

## Acceptance

- [ ] Given a graph and annotation filter, returns expected entity subset and additive edges
- [ ] Document chosen edge-inclusion rule (e.g. both endpoints in set) under design if finalized here
- [ ] Tests for multi-annotation / partial-match cases as applicable
