# WI-001 — Product design + glossary

**Story:** [`STORY.md`](STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md) G-P*  

## Goal

Lock non-technical product vocabulary and Journey 1–3 acceptance criteria. Update durable design under `docs/design/sbom/` so implementers and UI copy share one glossary.

## Personas (normative)

| Persona | Chrome | Owns |
|---------|--------|------|
| **Application owner** | **Applications** tab | Apps, drafts, versions, assets, CDX export |
| **Portfolio owner** | **Portfolios** tab | Portfolio taxonomy + **MI reports only here** |

Visual split only — **no auth / roles**. UI must stay **clean and obvious** (see WI-009).

## Deliverables

- [ ] Glossary: Application, Application version, Edit draft, Asset, Relation, Owning application, Depends on (app), Shared asset, Duplicate, Portfolio, Subject area, Application owner, Portfolio owner, MI reports (portfolio-scoped), CycloneDX export (demo)
- [ ] Resolve or defer open G-P* in `GAPS.md` (G-P11/G-P12 already locked — G-P12 rewritten 2026-08-13)
- [ ] Rewrite / extend [`docs/design/sbom/example.md`](../../../design/sbom/example.md): Applications vs Portfolios; MI under Portfolios only; linear report UX; clean UI
- [ ] Align domain services with hybrid persistence (SBOM app tables + objs assets/BOM)
- [ ] Domain API sketch in product language (no BoM* names in public shapes); MI API inputs = portfolio + level + report id

## Out of scope

- Foundation API changes
- UI implementation (WI-009)

## Acceptance

- Product language locked; open gaps marked resolved/deferred
- Example design doc matches Journeys 1–3 (personas + rewritten MI)
