# WI-004 — Living docs

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-003, WI-005  

## Goal

Align design docs with shipped catalog persistence, seeds (incl. Drop* / `key` / fail-ledger), REPLACE export, and workbench export. Keep C-27 result sketch clearly out of C-28.

## Scope

- [x] [`repository.md`](../../../design/policy/repository.md) — `key` identity, JPA (WI-001)
- [x] Policy overview seeds section — kinds, apply/replace, Drop*, user order (WI-001)
- [x] [`seeds.md`](../../../design/graph/seeds.md) — policy kinds pointer; REPLACE/Drop vs graph MERGE-only (WI-001)
- [x] [`workbench.md`](../../../design/policy/workbench.md) — full policy-setup export
- [x] [`RESULTS-MODEL.md`](../../completed/20260905-policy-suites/RESULTS-MODEL.md) — results ≠ C-28 (earlier boundary)
- [x] SEQUENCE / MILESTONE / C-29 **Before** cross-links (WI-000)

## Out of scope

- New production features
- Implementing result persistence

## Acceptance

- [x] Docs match shipped catalog store + seed + export contracts
- [x] Readers cannot confuse C-28 with evaluation-result Flyway
