# WI-001 — Registry seeds import/export

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Registry seeds  
**Status:** planned  
**Depends on:** WI-000

## Goal

Add ontology seed I/O under `/api/v1/objs/registry/import` and `/export` with `format=seeds`, catalog kinds only.

## Scope

- `POST /api/v1/objs/registry/import?format=seeds` (multipart)
- `GET /api/v1/objs/registry/export?format=seeds` → catalog YAML only
- Reject unknown `format` and non-catalog seed kinds on import
- Kind-scoped import helper on `SeedImporter` (allowed kinds)
- Controller tests for registry seeds I/O

## Out of scope

- Graph endpoints (WI-002)
- JSON Schema format (WI-003)
- Deleting `/seeds/**` (WI-002; may leave dual paths until then)

## Acceptance

- [ ] Registry export returns ObjectSchema + AllowedEdgeRule only
- [ ] Registry import rejects Graph documents
- [ ] `format` other than `seeds` → 400
