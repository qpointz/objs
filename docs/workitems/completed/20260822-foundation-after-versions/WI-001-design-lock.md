# WI-001 — Design lock

**Story:** [`STORY.md`](STORY.md)  
**Depends on:** WI-000  
**Status:** complete  
**Examples:** **docs**

Living design: pin reverse lookup, leftover matcher ops. Clocks and freeze/`clone()` split are **C-18** (do not re-lock clocks here). Align with C-18 ER.

## Docs (this WI)

- [x] [`docs/design/graph/pin-reverse-lookup.md`](../../../design/graph/pin-reverse-lookup.md) — union live + pin graphs in `listGraphIdsForEntity`; V5 index
- [x] [`docs/design/graph/matcher-pushdown-remainder.md`](../../../design/graph/matcher-pushdown-remainder.md) — `>`, prefix; not contains/`q`
- [x] [`docs/design/graph/apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md) — C-19 shipped rows
- [x] [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md) — pushdown remainder pointer
- [x] [`GAPS.md`](GAPS.md) — G-A1…G-A4 locked with doc links

## Out of scope

- Runtime code (WI-003+)
- C-20 contains/`q`
- Clocks (C-18)

## Acceptance

- Embedder can implement pin reverse + matcher remainder without reopening G-A3/G-A4
