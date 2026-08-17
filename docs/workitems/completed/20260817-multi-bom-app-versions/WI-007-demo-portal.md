# WI-007 — Demo seeder + Applications portal

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Demo + portal + living docs  
**Status:** done  
**Depends on:** WI-006

## Goal

Seed ~50% multi-constituent apps; enrich Applications portal cards; keep fingerprint/MI/CDX on aggregate; seed fingerprint name/category and a few parallel drafts.

## Deliverables

- [x] [`SbomDemoApps`](../../../examples/sbom/sbom-service/src/main/kotlin/org/poc/objs/sbom/demo/SbomDemoApps.kt) / [`SbomDemoInventorySeeder`](../../../examples/sbom/sbom-service/src/main/kotlin/org/poc/objs/sbom/demo/SbomDemoInventorySeeder.kt): 70 apps; ~half 1 constituent, ~half 2–3 (`Build` / `Runtime` / `Image`); deterministic
- [x] Seed app/version/constituent **tags** on a subset
- [x] Parallel drafts on a few apps (different targets); optionally one combined-from-multi draft
- [x] Demo fingerprints: `name` + `category` (`approval` \| `history` \| `unknown`); aggregate-only
- [x] Fingerprint / MI / depends-on / CDX smoke against aggregate; latest = semver-max RELEASED
- [x] [`ApplicationsPage.tsx`](../../../examples/sbom/sbom-service-ui/src/pages/ApplicationsPage.tsx): **content** = latest RELEASED + multi-BOM cue if that version has ≥ 2 BOMs; **footer** = total BOMs (all versions) · total versions; **lazy-load stats per app** (skeleton); same in list view
- [x] Seeder uses createDraft(target + based-on), not single-draft helpers

## Out of scope

- Design README polish (WI-008)

## Acceptance

- Demo profile shows mixed single/multi apps
- Portal cards match G-P4 / G-Q14 locks
