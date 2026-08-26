# WI-004 — Export docs + AGENTS cleanup

**Story:** [`STORY.md`](STORY.md)  
**Gaps:** G-7, G-8  

## Goal

Document the export workflow; automate or script post-export doc fixes in the **copy** (AGENTS workitems link).

## Tasks

- [x] [`scripts/export/README.md`](../../../scripts/export/README.md) — variables, derived names, excludes, verify checklist, tool deps, **`cleanup.yml` format**
- [x] Document: copy `cleanup.yml.template` → `cleanup.yml` and add destination-specific dirs/files to strip from export copy
- [x] Post-export step or generate-config hook: strip/replace `docs/workitems/` links in exported `AGENTS.md` and design docs
- [x] Brief pointer in root [`README.md`](../../../README.md) (source repo only)

## Acceptance

- [x] README sufficient for another developer to run export without reading story WIs
- [x] Exported copy: `rg 'docs/workitems/' docs/design AGENTS.md` → zero hits
- [x] G-7, G-8 closed or deferred with note in GAPS
