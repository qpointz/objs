# WI-004 — Validation APIs in core

**Story:** [`STORY.md`](STORY.md)  
**Status:** pending  
**Depends on:** WI-002

## Goal

Provide core validation per [`docs/design/graph/validation.md`](../../../design/graph/validation.md):

1. **Audit** — validate an in-memory graph against JSON Schemas and allowed type–role edge rules (non-blocking report).
2. **Persist gate** — APIs that enforce validation immediately before persistence (reject on failure); not invoked by pure SDK construction.

## Scope

- Payload validation against entity type JSON Schema
- Allowed-edge checks for entity types + role
- Minimal way to supply schemas / allowed-edge rules for tests (full registry productization TBD)
- Unit tests: valid accept / invalid reject / audit reports issues without mutating

## Out of scope

- Wiring into JPA flush (WI-005 hooks the gate)
- REST error model
- Update/delete policy beyond what create/persist needs (document assumptions)

## Acceptance

- [ ] Constructing invalid graphs in memory succeeds
- [ ] Persist-gate rejects invalid entity payload and disallowed edges
- [ ] Audit validation returns conformance info without requiring persistence
- [ ] Design open items updated if rule-declaration shape is chosen here
