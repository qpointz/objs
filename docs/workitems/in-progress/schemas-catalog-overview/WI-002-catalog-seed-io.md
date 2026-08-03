# WI-002 — Catalog seed export / import

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Catalog I/O  
**Status:** done  
**Depends on:** WI-000

## Goal

Export and import **catalog** definitions (ObjectSchema + AllowedEdgeRule) from the overview
toolbar using existing seed HTTP APIs.

## Scope

- `exportCatalogSeed()` → `GET /api/v1/objs/seeds/export` (no query params); download YAML
- `importCatalogSeed(file)` → `POST /api/v1/objs/seeds/import` multipart
- Reject files that contain `Graph` kind documents (clear error; do not apply)
- Show `SeedImportResult` per-document outcomes
- Refresh schema list + overview graph after successful import
- Document MERGE-only (no deletes) in UI copy / design docs (WI-003)

## Out of scope

- Graph seed documents in this UI
- Partial schemas-only / edges-only HTTP flags

## Acceptance

- [x] Export downloads catalog-only YAML
- [x] Import MERGE applies ObjectSchema / AllowedEdgeRule and refreshes overview
- [x] Files with Graph docs are rejected with a clear message
