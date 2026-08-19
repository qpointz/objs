# WI-007 — Living docs sweep

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 9 — Docs  
**Status:** complete  
**Depends on:** WI-002, WI-003, WI-004, WI-005, WI-006  
**Examples:** **docs** (workbench + SBOM + AR sweep; no new store API)

## Goal

Confirm living design matches **C-17 live lookups only** (no `q`). Catch leftover glue. **Not** the first write-up of each API (WI-002…006).

## Docs / sweep

- [x] [`docs/design/graph/apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md) uses shipped names; no stale stopgap rows for FB-1/FB-2/catalog/live-copy/`mergeGraph`/paging; `q` still points at C-20
- [x] [`FOUNDATION-BACKLOG.md`](../../completed/20260816-sbom-inventory-app/FOUNDATION-BACKLOG.md) — FB-1 / FB-2 **done**; FB-3 still open (C-20 / C-19)
- [x] [`docs/design/sbom/example.md`](../../../design/sbom/example.md) + [`examples/sbom/README.md`](../../../../examples/sbom/README.md)
- [x] [`docs/design/asset-repository/example.md`](../../../design/asset-repository/example.md) + [`examples/asset-repository/README.md`](../../../../examples/asset-repository/README.md) (collection copy + paging)
- [x] Sweep workbench + SBOM/AR for leftover stopgaps in [`EXAMPLES.md`](EXAMPLES.md) / [`WORKBENCH.md`](WORKBENCH.md)
- [x] [`EXAMPLES.md`](EXAMPLES.md) “today” column updated to “core API” where done

## Out of scope

- Story closure / MILESTONE completed bullet (user must ask)
- FB-3 contains/`q` (C-20) and remainder (`>`, regex, `tsvector`)
- SPA filter extraction (G-X4)

## Acceptance

- Design + mini-backlog + both example docs match code
- No example service still implements a store job this story shipped
