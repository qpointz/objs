# WI-002 — Core REPLACE mutate + tests

**Status:** done  
**Examples:** —  
**Depends on:** WI-008

## Goal

Implement named-graph **REPLACE** on `BoMNamedGraphStore` (and validate). Default **MERGE** unchanged.
Pool `BoMGraphStore.mutate` untouched for REPLACE (already on kind-first fields from WI-008).

## Behaviour

- `BoMMutateMode` on `BoMGraphMutation` (default `MERGE`)
- REPLACE: final membership + graph-local edges = `entities.set` + `edges.set`; prune extras
- Reject non-empty `*.unset` under REPLACE (`REPLACE_UNSET_NOT_ALLOWED`)
- Empty both `set` REPLACE clears contents; `graphId` kept
- Missing ids: allocate like MERGE
- One TX; validate final projection

## Acceptance

- [x] REPLACE matches `*.set` (incl. empty = clear)
- [x] REPLACE rejects non-empty `unset`
- [x] Pool entities preserved on detach
- [x] Stable `graphId`; works with `createDeepGraphVersion`
- [x] MERGE regression tests still pass
