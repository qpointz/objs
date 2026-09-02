# WI-003 — `:objs-policy-core` repository + pipeline

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Core orchestration + repository  
**Status:** planned  
**Depends on:** WI-002  
**Examples:** **—** (test fixtures only)

## Goal

Implement **`:objs-policy-core`**: PolicyRepository (+ suite store per lock), enrich → **select applicable** → evaluate orchestration, **suite collect + folder roll-up**, with a **stub/`CUSTOM` PolicyEngine** and a **test applicability selector**. Prove the pipeline without Drools.

## Scope

- [ ] Module skeleton; depends on `:objs-policy-api`
- [ ] PolicyRepository (+ suite) implementation (in-memory and/or JPA — per `G-P13` / `G-P31s`)
- [ ] Flyway/migrations only if locked in WI-001; respect objs vs app Flyway isolation ([`P-3`](../../completed/20260817-flyway-module-isolation/STORY.md))
- [ ] Default enricher chain wiring (empty / identity OK)
- [ ] Applicability step wiring; ship only what WI-001 allowed as foundation defaults (`G-P8`)
- [ ] `PolicyEvaluator` (name per lock): load policies → enrich → apply → dispatch engines → merge result
- [ ] **Suite execute:** collect members → evaluate → **roll up** node statuses (`G-P28s`–`G-P30s`)
- [ ] **Seed import** for Policy + Suite (MERGE per `G-P34seed`…`G-P40seed`); fixtures prefer YAML seeds
- [ ] **Batch / result pack** orchestration (`G-P41b`…): fan-out subjects; no cross-subject roll-up
- [ ] Refuse or allow ERROR fragments per `G-P17`
- [ ] Tests:
  - [ ] Applicable policy evaluated; N/A recorded with reason
  - [ ] N/A does not produce FAIL aggregate (per `G-P12` / `G-P29s`)
  - [ ] Mixed PASS/FAIL/ERROR behavior
  - [ ] Finding with empty bindings; finding with multiple entity ids; finding with edge ids
  - [ ] Suite tree: Database OK, API FAIL → root FAIL (E6 shape with fixtures)
  - [ ] M:N: same policy in two nodes evaluated once / placed twice per lock
  - [ ] Repository round-trip for policies and suites
  - [ ] Seed MERGE round-trip / load fixture suite from YAML (E7 shape)
  - [ ] Batch pack: two subjects, keyed results; no cross-subject aggregate status

## Boundaries

- Spring-free if that was locked; if Spring Data appears, keep it out of `:objs-policy-api`
- No Drools dependency
- No SBOM types; no product “IT Governance” content in `src/main`

## Out of scope

- Drools (WI-004)
- Example app wiring (WI-005)
- Workbench UI

## Acceptance

- [ ] `:objs-policy-core:test` green
- [ ] Cold-start Postgres scenario E1, suite roll-up E6, and **seed load E7** are expressible with stub engine + stub selector in tests
- [ ] Orchestration never evaluates policies marked N/A
- [ ] Suite roll-up matches locked aggregate formula
- [ ] Seed MERGE loads policies + suites per locked format
- [ ] Batch/result pack returns per-subject results only (E8 foundation half)
