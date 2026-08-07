# WI-002 — REST query params

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — REST  
**Status:** done  
**Depends on:** WI-001

## Goal

Map `dialect`, `includeEdges`, and `includeEdgePropertySchemas` query params on `GET /registry/export?format=json-schema` to options; document in OpenAPI; reject unknown values with 400.

## Scope

- Controller param mapping
- Controller tests
- OpenAPI / springdoc annotations as needed

## Out of scope

- UI

## Acceptance

- [x] Default GET (no extra params) still returns outbound catalog schema
- [x] `includeEdges=linked` returns inverse props
- [x] Unknown enum → 400
