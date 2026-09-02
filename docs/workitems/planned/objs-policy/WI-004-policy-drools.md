# WI-004 — `:objs-policy-drools` adapter

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Drools adapter  
**Status:** planned  
**Depends on:** WI-003  
**Examples:** **—** (fixture DRL under test resources only)

## Goal

Ship **`:objs-policy-drools`** as the first `PolicyEngine`: map (enriched) fragment → facts, compile/run policy evaluation bodies, emit unified findings/evidence. **No regulatory DRL** in the foundation module — fixtures only.

## Scope

- [ ] Module + Drools/KIE dependency per `G-P19`
- [ ] Fact mapping per `G-P18`
- [ ] Load DRL (or locked body format) from Policy artefact
- [ ] Session lifecycle / caching per `G-P20`
- [ ] Map engine outcomes → `EvaluationResult` entries (PASS/FAIL/ERROR) with **findings** whose `entities`/`edges` lists may be empty or multi-valued
- [ ] Tests with fixture policies (e.g. synthetic “version field must be > X” on a generic entity type; fixture that binds an edge)
- [ ] Confirm engine is invoked **only** for already-applicable policies (applicability stays in core)

## Boundaries

- Must not depend on SBOM/AR
- Must not place Drools on `:objs-policy-api` or force it onto `:objs-service`
- Fixture DRL must be obviously synthetic (names like `fixture-min-version`)

## Out of scope

- OPA
- Product policy packs
- REST

## Acceptance

- [ ] `:objs-policy-drools:test` green
- [ ] Core orchestration can dispatch `engineKind=DROOLS` to this adapter in integration-style module tests (as locked)
- [ ] No concrete PCI/Postgres product policy text in `src/main`
