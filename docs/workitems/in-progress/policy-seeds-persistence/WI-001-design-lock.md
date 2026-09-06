# WI-001 — Design lock (catalog persistence + seeds)

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-000  

## Goal

Confirm **catalog + seed** GAPS in [`GAPS.md`](GAPS.md) for C-28 (table already **resolved** 2026-09-06). Keep **evaluation result** / input-persist deferred (G-P11r / G-P32r). Refresh design notes for implementers.

## Scope

- [x] Resolve G-P13p, G-P14p, G-P34seed…G-P40seed (see GAPS decision log)
- [x] Boundary: no result tables / no input freeze in C-28
- [x] Draft/update design notes: `repository.md` (`key`), overview seeds, [`seeds.md`](../../../design/graph/seeds.md), `model.md`, `metadata.md`
- [x] Decision log entries

## Out of scope

- Implementing JPA / seed handlers / export (WI-002 / WI-003 / WI-005)
- Persisting `SuiteEvaluationResult` / `RESULTS-MODEL` tables

## Acceptance

- [x] All catalog/seed GAPS **resolved**
- [x] G-P11r / G-P32r remain **deferred**
- [x] Design notes sufficient to start WI-002 without re-debating GAPS
