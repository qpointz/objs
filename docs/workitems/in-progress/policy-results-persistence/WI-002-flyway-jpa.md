# WI-002 — Flyway + JPA archive tables

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-001 (GAPS closed)  

## Goal

Add **objs Flyway** vendor SQL + JPA for evaluation archives in **`:objs-persistence`**, aligned to [`RESULTS-MODEL.md`](../../completed/20260905-policy-suites/RESULTS-MODEL.md) and [`GAPS.md`](GAPS.md) (G-P11r, G-P46r).

## Scope

- [x] objs `V8` migrations for **postgresql** and **h2** (`V8__policy_evaluation_archive.sql`)
- [x] `objs_policy_evaluation` — id, kind, evaluated_at, overall_*, suite overlay, **tags/annotations JSON**, **`persist_profile` JSON** (axes/filters/preset/labeling), suite_tree / execution_context / input JSON
- [x] Outcome + finding tables (`objs_policy_finding.idx`; bindings as JSON id lists on finding)
- [x] Axis-optional context/input JSON columns
- [x] **No FK** to catalog tables (G-P46r); internal ON DELETE CASCADE only
- [x] JPA records + DAOs; registered in `ObjsPersistenceTestSupport.MANAGED_TYPES`
- [x] Flyway current-version asserts → `8`

## Out of scope

- Archive port / `save` API (WI-003)

## Acceptance

- [x] Migrations apply on H2 (Flyway autoconfigure + persistence harness tests)
- [x] Schema supports STANDARD (results+context, no input) and FULL (+ pack)
- [x] Catalog Drop not blocked by archive FKs
