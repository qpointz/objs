# WI-003 — Registry refresh REST + living docs

**Status:** done  
**Examples:** docs

## Goal

Expose `POST /api/v1/objs/registry/refresh` to force rehydrate both catalogs from PostgreSQL.
Update persistence / seeds / REST design docs.

## Acceptance

- [x] Refresh endpoint returns counts after rehydrate
- [x] In-memory catalogs no-op refresh (still return current counts)
- [x] `persistence.md`, `seeds.md`, `rest-api.md` describe TTL + refresh
- [x] MockMvc test for refresh
