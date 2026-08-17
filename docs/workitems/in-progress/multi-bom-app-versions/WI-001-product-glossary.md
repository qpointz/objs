# WI-001 — Product design + glossary

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Product + graph design  
**Status:** done  
**Depends on:** WI-000

## Goal

Lock remaining G-Q* and rewrite the SBOM example glossary so it matches [`STORY.md`](STORY.md) (multi-BOM, multi-draft, tags, fingerprint name/category).

## Deliverables

- [x] All G-Q* in [`GAPS.md`](GAPS.md) resolved or deferred (none left open)
- [x] Update [`docs/design/sbom/example.md`](../../../design/sbom/example.md):
  - Application / Version (target + based-on) / **BOM** (incomplete) / **Combined SBOM** (complete)
  - New application: **required target version**
  - Tags on app, version, BOM; **App+Ver+BOMs** unique union
  - New draft: optional **combine into a single BOM**
  - Fingerprint: **name** + **category** (`approval` \| `history` \| `unknown`); always Combined SBOM
- [x] Align journeys with progressive disclosure, **Create BOM**, New draft modal, Fingerprint modal, Applications portal stats

## Out of scope

- Engineer graph mapping detail (WI-002)
- Code

## Acceptance

- Glossary matches `STORY.md` locks
- No critical `open` GAPS remaining for Stage 2 (or explicitly deferred)
