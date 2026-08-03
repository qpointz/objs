# WI-004 — UI paths + design docs

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — UI + docs  
**Status:** planned  
**Depends on:** WI-002, WI-003

## Goal

Point the Schemas overview at the new registry seeds URLs and update design/REST docs.

## Scope

- `objs-sbom-example/ui/src/api.ts` catalog import/export URLs
- `docs/design/graph/seeds.md`, `docs/design/service/rest-api.md`, `docs/design/graph/object-schema-dsl.md`, `docs/design/ui.md` as needed
- OpenAPI configuration aligned with removed seeds group

## Out of scope

- JSON Schema download UI
- Graph explorer seed import UI

## Acceptance

- [ ] Overview Export/Import use `/registry/.../format=seeds`
- [ ] Design docs describe separated registry vs graph I/O and json-schema export
