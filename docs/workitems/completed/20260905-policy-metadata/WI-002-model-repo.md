# WI-002 — Policy metadata model + category repository

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  
**Depends on:** WI-001  

## Goal

Extend `:objs-policy-api` / `:objs-policy-core` with category vocabulary + Policy tags/category/annotations/semver and in-memory query/select. Flat `evaluate` unchanged.

## Scope

- [x] `Category` (+ write DTO) and `CategoryRepository` (in-memory)
- [x] Extend `Policy` / `PolicyWrite` with categoryId, tags, annotations, major/minor
- [x] Policy list/filter via `PolicyQuery` (category, tags, annotations, name)
- [x] Unit tests (CRUD categories; assign/validate; query; delete-in-use)

## Out of scope

- HTTP / workbench (WI-003)
- JPA / seeds (C-28)
- Suites (C-27)

## Acceptance

- [x] Categories are user-manageable in-memory; policies reference known categories
- [x] Query supports navigation filters locked in WI-001
- [x] Existing evaluate/Drools/service tests green
