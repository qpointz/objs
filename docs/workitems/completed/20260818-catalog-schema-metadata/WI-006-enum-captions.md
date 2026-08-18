# WI-006 — Enum value captions

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2–3 (DSL + all object UIs)  
**Status:** complete  
**Depends on:** WI-001

## Goal

Give ENUM values an optional UI **caption**. Stored `value` stays technical; `description` stays the long explanation; `caption` is what dropdowns and other object editors show.

## Deliverables

- [x] `BoMEnumValue.caption: String?` — trim; blank → omit. `value` and `description` still required and nonblank
- [x] JSON Schema projection: `x-objs-enumCaptions` map for values that have a caption (omit when none)
- [x] Seeds / DSL / json-schema-to-seeds docs
- [x] Workbench schema visual editor: caption field on enum values
- [x] Object editors use `caption` if set, else `value`: workbench `SchemaInstanceForm`, SBOM + AR `SchemaInstanceForm` / payload views / create-asset enum selects
- [x] Tests for normalize + projection; existing enum seeds remain valid without caption

## Out of scope

- Using caption in persist/validation (stored payload is still `value`)
- Renaming `description` or making it optional
- Entity graph cards that have no schema context (keep stored value)

## Acceptance

- Seed without `caption` behaves as today; UI shows `value`
- Seed/workbench with `caption: Low` and `value: LOW` stores `LOW` and shows **Low** in object editors
