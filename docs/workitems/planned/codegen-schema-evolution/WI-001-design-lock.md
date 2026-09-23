# WI-001 — Design lock

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design lock  
**Status:** done  
**Depends on:** WI-000  
**Examples:** **docs**

## Goal

Close every **open** row in [`GAPS.md`](GAPS.md) (resolve or defer). Align [`DESIGN.md`](DESIGN.md) and living design docs under `docs/design/graph/` with those locks. Do **not** start runtime/codegen code in this WI.

**Locked story width:** **L2 payload / single-entity migration only.** Compaction (L1) and decomposition (L3) are **G-X8 deferred**.

## Deliverables

- [x] All `open` GAPS closed or explicitly deferred with rationale ([`GAPS.md`](GAPS.md))
- [x] `DESIGN.md` marked aligned with locked decisions (kinds, fallback, evidence/examine, packs)
- [x] Living design doc(s) updated ([`schema-evolution.md`](../../../design/graph/schema-evolution.md), [`api-and-codegen.md`](../../../design/graph/api-and-codegen.md))
- [x] STORY normative table updated to match locks
- [x] Implementation WIs (WI-002+) adjusted for locked scope

## Out of scope

- `objs-api` / `objs-codegen-java` / example product code
- L1/L3 SPI design (G-X8)
