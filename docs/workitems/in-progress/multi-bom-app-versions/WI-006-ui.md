# WI-006 — Progressive UI + drafts + metadata

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — UI  
**Status:** done  
**Depends on:** WI-005

## Goal

Update [`ApplicationDetailPage.tsx`](../../../examples/sbom/sbom-service-ui/src/pages/ApplicationDetailPage.tsx) for multi-draft, progressive multi-BOM chrome, tags, and fingerprint create.

## Deliverables

- [x] [`ApplicationFormPage.tsx`](../../../examples/sbom/sbom-service-ui/src/pages/ApplicationFormPage.tsx): **required target version** (and tags)
- [x] Application + version **tags** editors on overview (DRAFT version tags editable). Combined unique set **below app name**, view mode, read-only (G-Q16) — not on Combined SBOM paper, not on portal
- [x] DRAFT overview: editable **target version** (rename, uniqueness); RELEASED version string read-only
- [x] New draft modal: based-on picker includes **RELEASED**, **DRAFT**, and **fingerprints** + target; combine prompt only if based-on **version** has **>1** BOM
- [x] Promote modal: **re-type** version to confirm; may override draft target if unique
- [x] Versions list: draft target + status + based-on (version or fingerprint name); snapshot menu lists drafts, released versions, and fingerprints
- [x] Fingerprint **Button** (not a link; match New draft/Save, not `subtle` next to an Anchor) opens modal: required **name** + **category** (`approval` / `history` / `unknown`); list/menu show name + category
- [x] **Hide** CycloneDX download link on application detail
- [x] Versions list: delete **DRAFT** (confirm; if dependents, list them then cascade — G-Q12); BOMs list: delete BOM (confirm; not Combined SBOM; not last)
- [x] **Create BOM** on overview (DRAFT); modal name/description/tags; button above BOMs list
- [x] Count ≥ 2: BOMs list (Combined SBOM + indented BOMs), Open badge; left-pane **multi-select**; `sbom=` query (`combined` = select all)
- [x] Left-pane compact BOM switch (Schema ▾ pattern); root `{app}` or `{app} / {bom}` or Combined SBOM
- [x] BOM metadata edit when exactly one BOM selected + DRAFT
- [x] Count = 1: no multi chrome (as today); app/version tags still visible; Create BOM still available on DRAFT overview. After delete 2→1: chrome off immediately; force remaining BOM; drop `sbom=` (G-Q10)
- [x] Fingerprint view: no BOMs list / no BOM switch
- [x] Unsaved edits (`versionDirty` / `payloadUnapplied`): **block** version, fingerprint, and BOM / Combined SBOM / multi-select switch; confirm **Stay** (keep) or **Leave** (discard then switch) (G-Q15)

## Out of scope

- Applications portal cards (WI-007)
- Demo data (WI-007)

## Acceptance

- Single-BOM apps look like pre-story UI except app/version tags and new-draft/fingerprint modals
- Multi-BOM and multi-draft flows match `STORY.md` / GAPS locks
