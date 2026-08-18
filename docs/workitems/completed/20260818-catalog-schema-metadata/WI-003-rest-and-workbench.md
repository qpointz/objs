# WI-003 — Registry REST + workbench edit forms

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — REST + workbench  
**Status:** complete  
**Depends on:** WI-002

## Goal

Expose metadata on registry APIs and let authors edit it in the workbench (schema envelope, fields, allowed edges).

## Deliverables

- [x] Registry DTOs / PUT-POST bodies include new fields; OpenAPI follows
- [x] [`ObjectEdgesEditor.tsx`](../../../../objs-service-ui/src/ObjectEdgesEditor.tsx) — description, verbs, tags, key/value
- [x] [`SchemaVisualBuilder.tsx`](../../../../objs-service-ui/src/SchemaVisualBuilder.tsx) — format **text** input; field tags + attributes
- [x] [`SchemaExplorerPage.tsx`](../../../../objs-service-ui/src/SchemaExplorerPage.tsx) — envelope tags + attributes on save/load
- [x] Types + expert YAML/JSON round-trip
- [x] Optional: relationship graph display label uses `sourceVerb` when set (identity remains `role`)
- [x] Tests for REST + UI helpers as existing patterns allow

## Out of scope

- Example seed content (WI-004)
- Example schema-browse allowed-edges UI (WI-005)

## Acceptance

- Workbench create/edit/reload preserves metadata
- Format is not a closed select list
- `./gradlew :objs-service:test` (and UI tests if present for these files)
