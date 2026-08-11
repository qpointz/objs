# WI-004 — Identity map projection

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Identity projection  
**Status:** done  
**Depends on:** WI-003  
**Modules:** `:objs-core` (+ unit tests)

## Goal

Provide a pure function that builds the comparable identity content of an entity payload from its schema.

## Contract

| | |
|--|--|
| **Input** | `contentSchema` (`BoMSchemaNode` OBJECT root) + payload `Map<String, Any?>` |
| **Output** | Flat `Map<String, Any?>` |
| **Keys** | Dotted paths from root for every `identifier: true` leaf |
| **Absent leaf** | Omit the key |
| **Arrays** | Never enter `items` |

## Acceptance

- [x] Unit tests: nested OBJECT, mixed identifier/non-identifier, empty identifiers → empty map, ARRAY siblings skipped
- [x] Deterministic key set / stable map suitable for equality checks in WI-005 (G-5)
