# WI-006 — Progressive UI + drafts + metadata

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — UI  
**Status:** pending  
**Depends on:** WI-005

## Goal

Update [`ApplicationDetailPage.tsx`](../../../examples/sbom/sbom-service-ui/src/pages/ApplicationDetailPage.tsx) for multi-draft and progressive multi-SBOM chrome.

## Deliverables

- [ ] New draft modal: based-on + target version; Versions list shows draft targets
- [ ] Create SBOM on overview (DRAFT); modal name/description/tags
- [ ] Count ≥ 2: SBOMs list (Combined + indented constituents), Open badge, `sbom=` query
- [ ] Left-pane compact SBOM switch; root `{app}` / `{app} / {sbom}`
- [ ] Constituent metadata edit when constituent Open + DRAFT
- [ ] Count = 1: no multi chrome (as today)
- [ ] Mutating BOM actions only for open constituent on DRAFT

## Out of scope

- Applications portal cards (WI-007)
- Demo data (WI-007)

## Acceptance

- Single-BOM apps look like pre-story UI
- Multi-BOM and multi-draft flows match `STORY.md` / GAPS locks
