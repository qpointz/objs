# WI-004 — SBOM `replaceBom` rewire + AR audit

**Status:** done  
**Examples:** SBOM + AR  
**Depends on:** WI-002  
**Parallel with:** WI-003 (does **not** need REST; prefer before WI-003 in tracker)

## Goal

Rewire SBOM `ApplicationVersionService.replaceBom` to named-graph **REPLACE** (stable BOM `graphId`)
via Kotlin/`bomMutation { setAll(…) }` — story driver dogfood before HTTP verb flip.
Audit asset-repository for exact-set sync; change only if needed.
Note uber-graph rebuild + `createDeepGraphVersion` pattern (no full analytics product).

## Acceptance

- [x] `replaceBom` uses REPLACE in one TX (set-only body, e.g. `bomMutation { setAll(…) }`)
- [x] Existing SBOM tests green
- [x] AR: migrated `ObjectWriteService` to kind-first MERGE mutate (exact-set sync still MERGE, not REPLACE)
- [x] Short note on uber-graph + deep version (below)

## Uber-graph pattern (note)

Long-lived analytics graph: keep a stable `graphId`, each rebuild
`namedGraphs.mutate(id, bomMutation { mode(REPLACE); setAll(desired) })`, then optionally
`namedGraphs.createDeepGraphVersion(id, …)` to pin history on the same id.
