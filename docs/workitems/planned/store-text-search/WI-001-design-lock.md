# WI-001 — Design lock

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design lock  
**Status:** planned  
**Depends on:** WI-000  
**Examples:** **docs** (workbench + SBOM + AR mapping; no Java/Kotlin)

## Goal

Close every **open** row in [`GAPS.md`](GAPS.md). Until this WI is `[x]`, do not implement WI-002.

This story exists because contains/`q` is **not** a small add-on to C-17 paging: DSL, field set, and SQL strategy need an explicit contract.

## Docs (this WI)

- [ ] Resolve G-A1…G-A7 (API shape, fields, match semantics, SQL, scope, empty schema, live vs snapshot)
- [ ] [`docs/design/graph/apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md) — FB-3 contains/`q` is **C-20**, not C-17; remainder still C-19
- [ ] [`docs/design/sbom/example.md`](../../../design/sbom/example.md) — asset `q` vs application `LIKE` (contract only)
- [ ] [`docs/design/asset-repository/example.md`](../../../design/asset-repository/example.md) — object `q` vs collection-name `LIKE`
- [ ] [`EXAMPLES.md`](EXAMPLES.md) method/param names once locked
- [ ] Touch matcher/persistence design only if the chosen DSL needs a new `obj-expr` operator

## Out of scope

- Runtime code (WI-002+)
- `tsvector`
- FB-3 operators other than contains/`q` (C-19)

## Acceptance

- An embedder could implement one store API without reopening G-A1…G-A7
- C-17 paging remains the page/size contract; this WI only says how `q` (or contains) combines with it
