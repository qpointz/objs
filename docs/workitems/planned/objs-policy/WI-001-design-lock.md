# WI-001 — Design lock

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design lock  
**Status:** planned  
**Depends on:** WI-000  
**Examples:** **docs** (no Java/Kotlin production modules)

## Goal

Close every **`open`** row in [`GAPS.md`](GAPS.md) (resolve or explicitly defer). Until this WI is `[x]`, do **not** implement WI-002+.

This story is philosophical and boundary-heavy: naming, applicability, repository vs graph entities, and engine layering must be locked like [`graph-frontend-jgrapht` WI-000](../../completed/20260903-graph-frontend-jgrapht/WI-000-design-lock.md).

## Scope (docs)

### Philosophy and boundaries

- [ ] Confirm family name `objs-policy*` (reject assessment-as-module-name)
- [ ] Confirm foundation vs product table in `STORY.md` / design doc
- [ ] Confirm foundation Policy ≠ SBOM ontology Policy (`G-P24`)
- [ ] Confirm non-goals: scoring product, default `:objs-service` dependency, replacing validation

### Pipeline and SPIs

- [ ] Lock `ApplicabilitySelector` shape (`G-P7`, `G-P8`) and per-policy applicability artefact (`G-P6`)
- [ ] Lock enricher contract and enrich-vs-apply order (`G-P9`, `G-P10`)
- [ ] Lock orchestrator entry API (`G-P15`) including **thin batch / result pack** (`G-P41b`…`G-P43b`); matrix stays product (`G-P43`)
- [ ] Lock behavior when fragment has ERROR diagnostics (`G-P17`)

### Model and result

- [ ] Lock Policy fields: identity/version (`G-P3`), body (`G-P4`), `engineKind` (`G-P5`)
- [ ] Lock **PolicySuite** model: nodes, M:N membership (`G-P26s`, `G-P27s`), execute scope (`G-P28s`)
- [ ] Lock **folder roll-up** + dedupe (`G-P29s`, `G-P30s`) and suite/node applicability (`G-P33`)
- [ ] Lock `EvaluationResult` / suite result + aggregate rules (`G-P11`, `G-P12`, `G-P16`)
- [ ] Lock **Finding** model: severity/message + **`entities` 0..n** + **`edges` 0..n** (`G-P11f`, `G-P12f`)
- [ ] Lock **seed format** for Policy + Suite: envelope, kinds, MERGE keys, import path (`G-P34seed`…`G-P40seed`)
- [ ] Lock PolicyRepository (+ suite store) SPI + backing-store approach (`G-P13`, `G-P14`, `G-P31s`, `G-P32s`)

### Modules and Drools boundaries

- [ ] Lock module map + packages (`G-P1`, `G-P2`, `G-P25`)
- [ ] Lock Drools fact-model *boundaries* and version approach (`G-P18`–`G-P20`) — enough for WI-004 without reopening philosophy
- [ ] Lock REST / example consumer / workbench in or out (`G-P21`–`G-P23`)

### Docs to update in this WI

- [ ] [`docs/design/policy/overview.md`](../../../design/policy/overview.md) — promote draft → normative design (layering, **suites**, **seeds**, pipeline, SPIs, non-goals; keep diagrams)
- [ ] Seed format living doc section or `docs/design/policy/seeds.md` once kinds/keys locked
- [ ] [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md) — cross-link policy kinds if shared envelope / handler SPI
- [ ] [`docs/design/graph/fragments-and-analysis.md`](../../../design/graph/fragments-and-analysis.md) — add policy as another resolved-fragment consumer (short subsection)
- [ ] [`docs/design/graph/apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md) — missing/planned policy row
- [ ] [`GAPS.md`](GAPS.md) — every former `open` row `resolved` or `deferred`; fill Decision log
- [ ] [`EXAMPLES.md`](EXAMPLES.md) — align wording with locked SPI names (keep **E6** roll-up example)
- [ ] [`STORY.md`](STORY.md) — flip “provisional” tables to “locked” where decided

## Out of scope

- Creating Gradle modules or Drools dependencies (WI-002+)
- Writing real regulatory policies or product suite trees (“IT Governance” content)
- OPA adapter implementation (unless deferred with an explicit stub-only note)

## Acceptance

- An implementer can build WI-002…WI-004 without reopening G-P1…G-P25, G-P26s…G-P33, G-P11f/G-P12f, G-P34seed…G-P40seed, or G-P41b…G-P43b
- Applicability remains an explicit pipeline step; suites + folder roll-up + **findings↔nodes/edges** + **seed format** + **thin batch pack** (no matrix) are in the normative design doc
- No production code required for this WI to complete
