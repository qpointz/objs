# WI-005 — Living docs

**Story:** [`STORY.md`](STORY.md)  
**Depends on:** WI-003, WI-004  
**Status:** complete  
**Examples:** **docs**

## Docs

- [x] [`docs/design/graph/pin-reverse-lookup.md`](../../../design/graph/pin-reverse-lookup.md) — shipped
- [x] [`docs/design/graph/matcher-pushdown-remainder.md`](../../../design/graph/matcher-pushdown-remainder.md) — shipped
- [x] [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md) — V5 index + pushdown pointer
- [x] [`docs/design/graph/apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md) — C-19 shipped rows
- [x] [`docs/design/sbom/example.md`](../../../design/sbom/example.md) — filter operators + pin-aware usage
- [x] [`FOUNDATION-BACKLOG.md`](../../completed/20260816-sbom-inventory-app/FOUNDATION-BACKLOG.md) — FB-3 partial
- [x] [`SEQUENCE.md`](../../SEQUENCE.md) — C-19 done note

## Verification

- [x] `./gradlew :objs-core:test :sbom-service:test` (SBOM without UI npm if file lock)
