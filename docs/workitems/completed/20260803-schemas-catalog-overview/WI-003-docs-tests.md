# WI-003 — Docs + smoke tests

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Docs  
**Status:** done  
**Depends on:** WI-000, WI-001, WI-002

## Goal

Document the Schemas overview and catalog I/O; add focused unit tests for pure helpers.

## Scope

- Update [`docs/design/ui.md`](../../../design/ui.md) Schemas section (overview vs detail)
- Note in [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md) that Overview uses
  catalog-only export/import; fix stale `apiVersion` example to `objs.poc.org/v1`
- Vitest: ontology graph element builder; Graph-kind import filter

## Acceptance

- [x] Design docs match shipped UI
- [x] Unit tests cover builder + import filter
