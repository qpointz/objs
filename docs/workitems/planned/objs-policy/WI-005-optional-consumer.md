# WI-005 — Optional HTTP and/or example consumer

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Optional consumer  
**Status:** planned (**explicitly gated** by WI-001 `G-P21` / `G-P22`)  
**Depends on:** WI-004  
**Examples:** **gated** (SBOM or minimal demo only if locked in)

## Goal

Prove an end-to-end **consumer** path without putting product rules in foundation. Exact shape is chosen in WI-001:

- **A.** Skip this WI (document deferral; story acceptance uses tests only), or
- **B.** Optional `:objs-policy-service` REST (capability-style, opt-in runner dependency), and/or
- **C.** Thin SBOM (or other example) hook: example-owned policies **and suite tree** + applicability selector calling core.

## Scope (only what WI-001 locked)

- [ ] If REST: module, endpoints, OpenAPI tag, absence semantics when module missing; **not** on `:objs-service` by default
- [ ] If SBOM/example: demo/fixture policies **+ suite seeds** under example tree; selector for “type present”; call evaluate / evaluateSuite on a Combined SBOM / selection fragment
- [ ] Document how product policies **and suites** are authored/loaded via **seeds** (example README or design note)
- [ ] Tests for the chosen consumer path

## Out of scope

- Full compliance product UI
- Migrating ontology `Policy` entities into PolicyRepository
- OPA

## Acceptance

- [ ] Matches WI-001 lock for G-P21/G-P22 (including “deferred / skipped” as a valid completion)
- [ ] If implemented: user-defined (example) policy **suite** + applicability demonstrated; foundation remains free of regulatory text
