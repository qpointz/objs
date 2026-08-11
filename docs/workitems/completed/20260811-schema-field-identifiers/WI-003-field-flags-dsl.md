# WI-003 — `identifier` + `searchable` DSL and schema editor

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Field flags  
**Status:** done  
**Depends on:** WI-002  
**Modules:** `:objs-core`, UI (`SchemaVisualBuilder`, types), design already in WI-001

## Goal

Add two independent field booleans to the object-schema DSL and expose them in the schema visual editor.

```kotlin
val identifier: Boolean = false
val searchable: Boolean = false
```

## Placement (normalizer)

Allowed when `field.schema.type` ∈ `STRING` | `NUMBER` | `INTEGER` | `BOOLEAN` | `ENUM`.

Forbidden:

- on `ARRAY` or `OBJECT` fields themselves (mark scalar leaves under nested OBJECTs)
- on any field whose path is under an `ARRAY` `items` schema

Defaults `false`; omit from JSON when false. Project JSON Schema extensions:

- `x-objs-identifier: true`
- `x-objs-searchable: true`

## UI

In [`SchemaVisualBuilder.tsx`](../../../../objs-service/ui/src/SchemaVisualBuilder.tsx): **Identifier** and **Searchable** checkboxes next to Required. Hide/disable for ARRAY/OBJECT field rows. Expert YAML/JSON already edits the document tree — no separate form behavior for instance payloads.

Do **not** change Composer/Explorer instance edit forms in this WI (`searchable` never does; `identifier` form lock is WI-005).

Flags apply to both **ENTITY** and **EDGE_PROPERTIES** schemas (G-1).

## Seeds

Leave flags `false` unless an obvious natural-key field is intentionally marked in the same WI (optional; not required for acceptance).

## Acceptance

- [x] Domain + API + seed round-trip preserve both flags
- [x] Normalizer rejects illegal placements
- [x] JSON Schema emits `x-objs-*` when true
- [x] Schema editor checkboxes work; UI types updated
- [x] Tests cover accept/reject cases
