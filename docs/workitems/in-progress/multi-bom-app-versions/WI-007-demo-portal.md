# WI-007 — Demo seeder + Applications portal

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Demo + portal + living docs  
**Status:** pending  
**Depends on:** WI-006

## Goal

Seed ~50% multi-constituent apps; enrich Applications portal cards; ensure fingerprint/MI/CDX paths use aggregate; optionally seed parallel drafts.

## Deliverables

- [ ] [`SbomDemoApps`](../../../examples/sbom/sbom-service/src/main/kotlin/org/poc/objs/sbom/demo/SbomDemoApps.kt) / seeder: ~half 1 constituent, ~half 2–3; deterministic
- [ ] Parallel drafts on a few apps (different targets) if useful
- [ ] [`ApplicationsPage.tsx`](../../../examples/sbom/sbom-service-ui/src/pages/ApplicationsPage.tsx): latest RELEASED pill; footer stats (SBOMs · versions)
- [ ] Fingerprint / MI / depends-on / CDX smoke against aggregate
- [ ] Draft create/promote paths updated for target + based-on

## Out of scope

- Design README polish (WI-008)

## Acceptance

- Demo profile shows mixed single/multi apps
- Portal cards match G-P4 / G-Q14 locks
