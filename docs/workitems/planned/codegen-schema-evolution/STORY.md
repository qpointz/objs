# Story: codegen-schema-evolution — typed multi-version deserialize + migrations

**Slug:** `codegen-schema-evolution`  
**Branch:** `codegen-schema-evolution`  
**Status:** ready for MR (WIs complete)  
**Folder:** [`docs/workitems/planned/codegen-schema-evolution/`](.)  
**Backlog:** [C-35](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Before:** [C-23 `objs-api-codegen`](../../completed/20260828-objs-api-codegen/STORY.md)  
**Design:** [`DESIGN.md`](DESIGN.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Living docs:** [`schema-evolution.md`](../../../design/graph/schema-evolution.md), [`api-and-codegen.md`](../../../design/graph/api-and-codegen.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)  
**Closes deferred:** C-23 [G-30](../../completed/20260828-objs-api-codegen/GAPS.md) (SBOM evolved Component + dual view)

## Goal

L2 payload / single-entity upgrades: latest-only OM, step kinds, additive fallback, evidence/examine dual wire for fingerprints, regression packs.

## Work Items

- [x] WI-000 — Story scaffold
- [x] WI-001 — Design lock
- [x] WI-002 — Runtime SPI + typed hydrate path
- [x] WI-003 — SBOM Component upgrade + fingerprint dual view
- [x] WI-004 — Living docs + G-30 close-out

## Acceptance

- [x] Older pins hydrate to latest when chain or additive fallback succeeds
- [x] Otherwise raw + diagnostic; Entity retained
- [x] Hand migrations outside generated trees
- [x] SBOM G-30 regression / dual-view proof passes
- [x] GAPS stay resolved or deferred

## Out of scope / deferred

G-E5, G-E6, G-E7, G-X8; full Lane B second-export codegen (SBOM used hand DTOs).

## Process notes

Do not archive this story until the user asks after MR merge.
