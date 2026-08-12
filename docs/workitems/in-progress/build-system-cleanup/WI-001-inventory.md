# WI-001 — Dependency + plugin inventory

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Inventory  
**Status:** done  
**Depends on:** WI-000

## Goal

Record keep/drop decisions for every catalog plugin and library, and per-module
direct dependencies, before mutating Gradle files.

## Deliverable

[`INVENTORY.md`](INVENTORY.md) — matrices + decisions used by WI-002…WI-004.

## Acceptance

- [x] Plugin matrix (all modules + catalog-only) with keep/drop  
- [x] Library catalog keep/drop  
- [x] Per-module direct-dep notes (`api` vs drop / transitive)  
