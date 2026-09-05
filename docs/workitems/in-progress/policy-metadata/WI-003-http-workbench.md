# WI-003 — HTTP + workbench Policy list navigation

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  
**Depends on:** WI-002  

## Goal

Expose category CRUD and policy list filters on `:objs-policy-service`; update workbench Policy play so the left list is **navigable** and the editor uses **General | Code** tabs.

## Scope

- [x] HTTP: category CRUD; policy list query params; create/update carry metadata
- [x] Workbench: category management; category picker; tags; annotations editor; major.minor
- [x] Workbench: Policy list navigate (category + tag + name filters)
- [x] Soft-fail / capabilities extended (`categories`, `query`)

## Out of scope

- Suite UI (C-27)
- Persistence (C-28)

## Acceptance

- [x] Operator can manage categories and navigate a larger policy list in workbench
- [x] Check/Evaluate remain flat evaluate against selected policy / context
- [x] UI build (`tsc` + vite) passes
