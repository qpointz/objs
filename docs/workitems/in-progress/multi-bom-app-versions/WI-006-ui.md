# WI-006 — Progressive UI + drafts + metadata

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — UI  
**Status:** pending  
**Depends on:** WI-005

## Goal

Update [`ApplicationDetailPage.tsx`](../../../examples/sbom/sbom-service-ui/src/pages/ApplicationDetailPage.tsx) for multi-draft, progressive multi-BOM chrome, tags, and fingerprint create.

## Deliverables

- [ ] [`ApplicationFormPage.tsx`](../../../examples/sbom/sbom-service-ui/src/pages/ApplicationFormPage.tsx): **required target version** (and tags)
- [ ] Application + version **tags** editors on overview (DRAFT version tags editable). Combined unique set **below app name**, view mode, read-only (G-Q16) — not on Combined SBOM paper, not on portal
- [ ] DRAFT overview: editable **target version** (rename, uniqueness); RELEASED version string read-only
- [ ] New draft modal: based-on picker includes **RELEASED**, **DRAFT**, and **fingerprints** + target; combine prompt only if based-on **version** has **>1** BOM
- [ ] Promote modal: **re-type** version to confirm; may override draft target if unique
- [ ] Versions list: draft target + status + based-on (version or fingerprint name); snapshot menu lists drafts, released versions, and fingerprints
- [ ] Fingerprint **Button** (not a link; match New draft/Save, not `subtle` next to an Anchor) opens modal: required **name** + **category** (`approval` / `history` / `unknown`); list/menu show name + category
- [ ] **Hide** CycloneDX download link on application detail
- [ ] Versions list: delete **DRAFT** (confirm; if dependents, list them then cascade — G-Q12); BOMs list: delete BOM (confirm; not Combined SBOM; not last)
- [ ] **Create BOM** on overview (DRAFT); modal name/description/tags; button above BOMs list
- [ ] Count ≥ 2: BOMs list (Combined SBOM + indented BOMs), Open badge; left-pane **multi-select**; `sbom=` query (`combined` = select all)
- [ ] Left-pane compact BOM switch (Schema ▾ pattern); root `{app}` or `{app} / {bom}` or Combined SBOM
- [ ] BOM metadata edit when exactly one BOM selected + DRAFT
- [ ] Count = 1: no multi chrome (as today); app/version tags still visible; Create BOM still available on DRAFT overview. After delete 2→1: chrome off immediately; force remaining BOM; drop `sbom=` (G-Q10)
- [ ] Fingerprint view: no BOMs list / no BOM switch
- [ ] Unsaved edits (`versionDirty` / `payloadUnapplied`): **block** version, fingerprint, and BOM / Combined SBOM / multi-select switch; confirm **Stay** (keep) or **Leave** (discard then switch) (G-Q15)

## Out of scope

- Applications portal cards (WI-007)
- Demo data (WI-007)

## Acceptance

- Single-BOM apps look like pre-story UI except app/version tags and new-draft/fingerprint modals
- Multi-BOM and multi-draft flows match `STORY.md` / GAPS locks
