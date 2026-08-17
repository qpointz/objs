# WI-001 — Product design + glossary

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Product + graph design  
**Status:** pending  
**Depends on:** WI-000

## Goal

Lock remaining G-Q* and rewrite the SBOM example glossary so it matches [`STORY.md`](STORY.md) (multi-BOM, multi-draft, tags, fingerprint name/category).

## Deliverables

- [ ] Resolve or defer **G-Q1…G-Q4**, **G-Q6…G-Q16** in [`GAPS.md`](GAPS.md) (G-Q5 and G-Q17 already resolved)
- [ ] Update [`docs/design/sbom/example.md`](../../../design/sbom/example.md):
  - Application / Version (target + based-on) / SBOM constituent / Combined SBOM
  - Tags on app, version, constituent; **App+Ver+SBOMs** unique union
  - New draft: optional **combine into a single BOM**
  - Fingerprint: **name** + **category** (`approval` \| `history` \| `unknown`); always Combined
- [ ] Align journeys with progressive disclosure, Create SBOM, New draft modal, Fingerprint modal, Applications portal stats

## Out of scope

- Engineer graph mapping detail (WI-002)
- Code

## Acceptance

- Glossary matches `STORY.md` locks
- No critical `open` GAPS remaining for Stage 2 (or explicitly deferred)
