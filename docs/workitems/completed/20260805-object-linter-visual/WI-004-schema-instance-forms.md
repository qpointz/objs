# WI-004 — Schema-driven instance forms

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Forms  
**Status:** done  
**Depends on:** WI-002

## Goal

Build a reusable schema-driven form for entity payloads (and edge properties) from registry DSL `contentSchema`, plus a free-form annotations editor.

## Scope

- `SchemaInstanceForm` recursive over OBJECT / ARRAY / STRING / NUMBER / INTEGER / BOOLEAN / ENUM
- Honor required, title, description, default, common formats, enum descriptions
- Annotations key/value string editor
- Edge properties form when allow-list policy is SCHEMA
- Unit tests for default-value helpers

## Out of scope

- RJSF / third-party form library

## Acceptance

- [x] Forms render from a real ENTITY schema DSL without hand-written fields
- [x] Nested objects and arrays are editable
- [x] Annotations editor round-trips string maps
