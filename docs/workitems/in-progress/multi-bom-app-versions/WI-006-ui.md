# WI-006 — Progressive UI + drafts + metadata

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — UI  
**Status:** pending  
**Depends on:** WI-005

## Goal

Update [`ApplicationDetailPage.tsx`](../../../examples/sbom/sbom-service-ui/src/pages/ApplicationDetailPage.tsx) for multi-draft, progressive multi-SBOM chrome, tags, and fingerprint create.

## Deliverables

- [ ] Application + version **tags** on overview (DRAFT version tags editable; Combined shows App+Ver+SBOMs unique set read-only)
- [ ] New draft modal: based-on + target; if based-on has **>1** SBOM, ask **combine into a single BOM**
- [ ] Versions list: draft target + status + based-on; snapshot menu lists all drafts and released versions
- [ ] Fingerprint modal: required **name** + **category** (`approval` / `history` / `unknown`); list/menu show name + category (not note/SHA)
- [ ] Create SBOM on overview (DRAFT); modal name/description/tags; button above SBOMs list
- [ ] Count ≥ 2: SBOMs list (Combined + indented constituents), Open badge, `sbom=` query
- [ ] Left-pane compact SBOM switch (Schema ▾ pattern); root `{app}` or `{app} / {sbom}`
- [ ] Constituent metadata edit when constituent Open + DRAFT
- [ ] Count = 1: no multi chrome (as today); app/version tags still visible; Create SBOM still available on DRAFT overview
- [ ] Fingerprint view: no SBOMs list / no constituent switch
- [ ] Mutating BOM actions only for open constituent on DRAFT

## Out of scope

- Applications portal cards (WI-007)
- Demo data (WI-007)

## Acceptance

- Single-BOM apps look like pre-story UI except app/version tags and new-draft/fingerprint modals
- Multi-BOM and multi-draft flows match `STORY.md` / GAPS locks
