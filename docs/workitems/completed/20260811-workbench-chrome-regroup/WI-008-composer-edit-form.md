# WI-008 — Composer edit form (payload / schema)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Composer / Query L2  
**Status:** done  
**Depends on:** WI-004  
**Modules:** `:objs-service` UI  
**Gaps:** G-U15 (defaults locked; subject to future review)

## Goal

Improve Composer selection edit form ([`SchemaInstanceForm.tsx`](../../../../objs-service/ui/src/SchemaInstanceForm.tsx), [`ObjectLinterVisualPanel.tsx`](../../../../objs-service/ui/src/ObjectLinterVisualPanel.tsx)):

1. **No redundant section titles** — drop inner **Payload** / **Annotations** `SectionChrome` headers when those labels already appear as tab text.
2. **Delete field from payload** (entity payload only) — omit the key (not `""`). Row stays with **(deleted)**; entering a value restores the key. Nested object keys and whole object/array fields supported the same way. Required fields may be deleted (Validate later).
3. **Schema ▾** — list **other versions of the same entity type** only (not cross-type); same type@version = no-op. **Migrate** key→key (recurse OBJECT); drop unmatched; confirm on **zero** and **partial**; on confirm apply version + migrated payload.

Do not build a general schema-diff engine or server migration API.

## Acceptance

- [x] Tab panels have no duplicate Payload/Annotations headings
- [x] Field delete omits key + (deleted) mark; `""` ≠ deleted; entity payload only
- [x] Schema picker + migrate with confirm on zero/partial; unit tests for migrate helper
