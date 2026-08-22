# WI-004 — FB-3 remaining operators

**Story:** [`STORY.md`](STORY.md)  
**Depends on:** WI-001  
**Status:** complete  
**Examples:** **workbench + SBOM + AR** if the operator is used there

Pushdown beyond C-17 equality and C-20 contains/`q`. Scope operators in this WI’s design lock (e.g. `>`, prefix). Not `tsvector` unless locked.

## Core

- [x] `BoMObjExprLowerer` — `>`, `>=`, `<`, `<=` on first-level `p.*`; prefix via `p.field =~ '^literal'`
- [x] `BoMObjExprPushdown` + `BoMPoolEntityReader` SQL (`payload ->>`, `LIKE`)
- [x] H2 falls back to local eval when scalar pushdown unavailable
- [x] `BoMCatalogSupport.filterMapToObjExpr` — trailing `*`, leading compare operators
- [x] Tests: matcher unit, pool select, SBOM prefix search

## SBOM

- [x] `AssetInventoryServiceTest.shouldSearchByPrefixFilter`

## Docs (same commit)

- [x] KDoc on pushdown types

## Acceptance

- `./gradlew :objs-core:test :sbom-service:test` (SBOM: exclude `:objs-service-ui:npmInstall` if UI file lock on Windows)
