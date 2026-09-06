# WI-005 — Catalog REPLACE export (+ workbench)

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-002, WI-003  

## Goal

Export the live durable catalog as **REPLACE** seeds for **all present** Category, Policy, and PolicySuite objects (G-P39seed), and expose a **workbench** action to download the full policy setup.

## Scope

### API / library

- [x] Export categories (REPLACE docs, by `key`)
- [x] Export policies with **inline** body (REPLACE docs; latest revision per key)
- [x] Export policy suites (REPLACE docs)
- [x] YAML re-importable via `SeedDocumentHandler`
- [x] Tests: export → reimport restores catalog

### Workbench (`:objs-service-ui` / policy play)

- [x] UI: **Export** full policy setup
- [x] Download REPLACE seed YAML
- [x] `GET /api/v1/objs/policy/export?format=seeds`

## Out of scope

- Evaluation result / history export (G-P11r)
- Emitting Drop* docs as part of full-setup export
- Product pack content authoring
- Seed **import** UI

## Acceptance

- [x] REPLACE seeds for catalog objects of the three kinds
- [x] Policy bodies inlined
- [x] Re-import of exported pack restores catalog per GAPS
- [x] Workbench one-action export of full policy setup
