# WI-001 — Export options + exporter

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Core options  
**Status:** pending  
**Depends on:** WI-000

## Goal

Add `BoMJsonSchemaExportOptions` and teach `FullCatalogJsonSchemaExporter` to honour dialect, edge inclusion (`none` / `outbound` / `linked`), and edge-property `$defs`.

## Scope

- Options types + wire parsing helpers
- Exporter `export(options)` with defaults matching today’s output
- Inverse relation props when `includeEdges=LINKED`
- Unit tests including Database↔Dataset linked shape

## Out of scope

- REST wiring
- UI

## Acceptance

- [ ] Default options reproduce prior outbound-only export (plus options echo marker)
- [ ] `NONE` omits relation props; `LINKED` adds inverse props
- [ ] Inverse naming and cardinality rules match STORY.md
