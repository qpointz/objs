# Story: Asset repository Postgres, SPA refresh, load batches

**Slug:** `asset-repository-ops-fixes`  
**Branch:** `fix/ar-collection-search-postgres`  
**Status:** completed  
**Folder:** [`docs/workitems/completed/20260814-asset-repository-ops-fixes/`](.)  
**Backlog:** [D-5](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Follows:** [`20260814-asset-repository-demo-seeds`](../20260814-asset-repository-demo-seeds/STORY.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Unblock the **postgres** profile collection list (`lower(bytea)`), serve **SPA deep-links** on refresh for `/app` and `/workbench`, and make **qsynth `load.py`** post objects and edges in composition batches.

## Work Items

- [x] WI-001 — PostgreSQL collection search casts (`WI-001-postgres-collection-search.md`)
- [x] WI-002 — SPA routing filters (`WI-002-spa-routing.md`)
- [x] WI-003 — Batch `load.py` compositions (`WI-003-load-py-batches.md`)

## Out of scope

- New GitLab issue
- Changing composition persist/validation cost on the server
