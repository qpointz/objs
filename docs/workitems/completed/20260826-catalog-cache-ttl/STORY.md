# Story: Catalog cache TTL

**Slug:** `catalog-cache-ttl`  
**Branch:** `catalog-cache-ttl`  
**Status:** completed  
**Closed:** 2026-08-26  
**Folder:** [`docs/workitems/completed/20260826-catalog-cache-ttl/`](.)  
**Backlog:** [C-21](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Design:** [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Hybrid **write-through + Caffeine TTL** for JPA schema / allowed-edge catalogs so external DB truncates
(or out-of-band catalog edits) become visible without process restart. Keep mid-transaction write-through
visibility for seed import. Add an explicit registry **refresh** REST for ops (e.g. Azure) when waiting
for TTL is not enough.

## Normative

| Topic | Lock |
|-------|------|
| Layer | `:objs-core` catalogs; workbench exposes refresh |
| Cache | Whole-catalog snapshot; `expireAfterWrite` TTL |
| Writes | DB first, then update snapshot + reset TTL clock |
| Mid-TX | Skip TTL reload while a transaction is active |
| Config | `objs.catalogs.cache-ttl` (default `30s`; `0` = no TTL expiry) |
| Refresh | `POST /api/v1/objs/registry/refresh` → rehydrate both catalogs from DB |

## Work Items

- [x] WI-001 — Story scaffold — examples: **—** (`WI-001-story-scaffold.md`)
- [x] WI-002 — Hybrid Caffeine TTL catalogs + tests — examples: **—** (`WI-002-ttl-cache.md`)
- [x] WI-003 — Registry refresh REST + living docs — examples: **docs** (`WI-003-refresh-and-docs.md`)

## Acceptance

- [x] External truncate of catalog tables is reflected after TTL (or immediate refresh) without restart
- [x] Seed import mid-TX still sees write-through registrations
- [x] `objs.catalogs.cache-ttl=0` preserves startup hydrate + write-through only
- [x] `./gradlew :objs-core:test :objs-service:test`

## Out of scope

- Per-key Caffeine (coherent `all()` / allow-list needs a snapshot)
- Distributed cache invalidation across replicas (TTL + refresh per instance)
- Changing MERGE seed semantics
