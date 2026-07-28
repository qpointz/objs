# WI-004 — Validation APIs in core

**Story:** [`STORY.md`](STORY.md)  
**Status:** pending  
**Depends on:** WI-002

## Goal

Provide core validation per [`docs/design/graph/validation.md`](../../../design/graph/validation.md):

1. **Audit** — validate an in-memory graph against JSON Schemas and allowed type–role edge rules (non-blocking report).
2. **Persist gate** — APIs that enforce validation immediately before persistence for **create, update, and delete** (reject on failure); not invoked by pure SDK construction.
3. **Batch subgraph write** — payload of entities + edges; **two-stage** gate: (1) validate entities vs schema, (2) validate edges against payload ∪ store **right before persist** (G-19).

## Scope

- Allowed-edge checks: in-memory allow-list of `(sourceType, role, targetType)` with **properties policy** (`none` bare edge vs `schema` + empty allowed/forbidden)
- Edge **properties** validation against central schema `(type, version)` when policy = `schema`
- Payload validation against central schema `(type, version)` for entities
- In-memory **central** schema catalog + allowed-edge rules (G-6, G-7, G-8)
- Unit tests: bare edge role, schema edge role, empty forbidden/allowed, deny unknown combo

## Out of scope

- Wiring into JPA flush (WI-005 hooks the gate)
- REST error model

## Acceptance

- [ ] Constructing invalid graphs in memory succeeds
- [ ] Persist-gate rejects invalid entity/edge on **create, update, and delete**
- [ ] No id → create (`UUID.randomUUID()` assigned); id present → update (unknown id rejects); delete explicit
- [ ] Two-stage batch: entity schema failures reject before edge checks; edge stage sees payload ∪ store
- [ ] Batch payload: edge to **existing** store entity validates using store entity type; missing endpoint rejects
- [ ] Audit validation returns conformance info without requiring persistence
