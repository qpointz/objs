# WI-003 — Schemas overview UI

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — UI  
**Status:** pending  
**Depends on:** WI-002

## Goal

Expose JSON Schema export options on the Schemas full-catalog overview (shared state for Text preview and Export).

## Scope

- `JsonSchemaExportOptions` + `exportCatalog(format, options?)` in `api.ts`
- Text tab options row when format is JSON Schema
- Export → JSON Schema uses the same options (hint label)

## Out of scope

- Per-type Schema JSON Schema tab options

## Acceptance

- [ ] Changing Include edges reloads Text preview
- [ ] Export download uses current options
