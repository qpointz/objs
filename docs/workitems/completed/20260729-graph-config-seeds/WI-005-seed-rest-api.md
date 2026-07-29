# WI-005 — Seed import/export REST API

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Seed implementation  
**Status:** done  
**Depends on:** WI-003; G-S10 and G-S11 in [`GAPS.md`](GAPS.md)

## Goal

Expose the common seed importer and canonical serializer over the foundation REST API without
introducing an unbounded graph dump.

## Endpoints

| Method | Path | Behavior |
|--------|------|----------|
| `POST` | `/api/v1/objs/seeds/import` | Multipart YAML (`file` part); shared transactional importer; no ledger write |
| `GET` | `/api/v1/objs/seeds/export` | Canonical YAML for schemas + rules; Graph included when annotation params present |
| `GET` | `/api/v1/objs/seeds/export/graph` | Same as export but requires a non-empty annotation filter |

## Acceptance

- [x] REST and startup imports have identical parsing, validation, and merge behavior
- [x] Successful import returns applied/skipped counts by document kind
- [x] Failed import returns actionable document location and validation details
- [x] Export emits canonical YAML covering all seed kinds present and accepted by the importer
- [x] Graph export requires an explicit bounded filter and cannot dump the entire graph
- [x] Adding a new seed kind later does not require changing the export endpoint contract
- [x] OpenAPI and MockMvc tests document and verify the endpoints
