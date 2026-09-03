# WI-002 — Store substring pushdown

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Store  
**Status:** planned — **not ready until WI-001**  
**Depends on:** WI-001; C-17 WI-006 paging  
**Examples:** **—** (consumers in WI-003)

## Goal

Implement the WI-001 store contract in `:objs-persistence` with H2 tests (Postgres IT if SQL-specific).

Draft from the old C-17 WI-008 (replace with WI-001 locks):

- [ ] Paged pool/graph select accepts locked search API
- [ ] Case-insensitive match on the locked field set
- [ ] Blank search → no extra predicate
- [ ] Tests: hit/miss, case, type-only, pool vs graph

## Out of scope

- Workbench / SBOM / AR rewire (WI-003)
- `tsvector`; remaining FB-3 operators

## Acceptance

- Core tests cover WI-001 semantics
- `./gradlew :objs-persistence:test`
