# WI-001 — Product design + glossary

**Story:** [`STORY.md`](STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md) G-P*  
**Status:** complete  

## Goal

Lock non-technical product vocabulary and Journey 1–3 acceptance criteria. Update durable design under `docs/design/sbom/` so implementers and UI copy share one glossary.

## Personas (normative)

| Persona | Chrome | Owns |
|---------|--------|------|
| **Application owner** | **Applications** tab | Apps, drafts, versions, assets, CDX export |
| **Portfolio owner** | **Portfolios** tab | Portfolio taxonomy + **MI reports only here** |

Visual split only — **no auth / roles**. UI must stay **clean and obvious** (see WI-009).

## Deliverables

- [x] Glossary: Application, Application version, Edit draft, Asset, Relation, Owning application, Depends on (app), Shared asset, Duplicate, Portfolio, Subject area, Application owner, Portfolio owner, MI reports (portfolio-scoped), CycloneDX export (demo)  
- [x] Resolve or defer open G-P* in `GAPS.md` (all G-P* product rows resolved / deferred / cancelled; G-F7/G-F8 remain foundation open)  
- [x] Rewrite [`docs/design/sbom/example.md`](../../../design/sbom/example.md): Applications vs Portfolios; MI under Portfolios only; linear report UX; clean UI  
- [x] Align domain services with hybrid persistence (SBOM app tables + objs assets/BOM)  
- [x] Domain API sketch in product language (no BoM* names in public shapes); MI API inputs = portfolio + level + report id  

## Out of scope

- Foundation API changes
- UI implementation (WI-009)

## Acceptance

- [x] Product language locked; open G-P* marked resolved/deferred  
- [x] Example design doc matches Journeys 1–3 (personas + rewritten MI)  
