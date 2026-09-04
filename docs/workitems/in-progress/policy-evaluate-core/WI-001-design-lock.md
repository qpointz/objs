# WI-001 — Design lock (S1 gaps only)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design lock  
**Status:** complete  
**Depends on:** WI-000  
**Examples:** **docs** (no production modules)

## Goal

Close every **`open`** row in **this** story’s [`GAPS.md`](GAPS.md). Do **not** lock Drools, suites, seeds, batch, or consumer gaps (those folders own them).

**Done:** Gap-by-gap confirm 2026-09-04 — all S1 rows `resolved`. See Decision log in [`GAPS.md`](GAPS.md).

## Scope

### Philosophy

- [x] Confirm `objs-policy*` naming and foundation vs product boundary for **flat** evaluate
- [x] Confirm foundation Policy ≠ SBOM ontology Policy (docs pointer; rename deferred G-P24)
- [x] Confirm non-goals for S1: suites, seeds, Drools, batch, REST, scoring

### Pipeline and SPIs (flat)

- [x] Lock applicability bound to evaluate (`G-P7`, `G-P8`) and optional applicability artefact (`G-P6`)
- [x] Lock context wiring + order (`G-P9`, `G-P10`) — SPI **`PolicyContextWirer`** / **PolicyContextWiring** (not Enricher)
- [x] Lock fixed orchestrator + wrappers later (`G-P15`)
- [x] Lock refuse-on-fragment-ERROR (`G-P17`)

### Model and result (flat)

- [x] Lock Policy fields (`G-P3`–`G-P5`)
- [x] Lock flat `EvaluationResult` + convenience aggregate (`G-P11`, `G-P12`, `G-P16`)
- [x] Lock Finding model (`G-P11f`, `G-P12f`)
- [x] Lock in-memory `PolicyRepository` SPI surface (`G-P13`, `G-P14`)

### Modules

- [x] Lock `:objs-policy-api` / `:objs-policy-core` + packages (`G-P1`, `G-P2`, `G-P25`)

### Docs in this WI

- [x] [`docs/design/policy/overview.md`](../../../design/policy/overview.md) — **Locked (S1)** section
- [x] [`GAPS.md`](GAPS.md) — all S1 rows `resolved`; Decision log filled
- [x] [`EXAMPLES.md`](EXAMPLES.md) — aligned with locks
- [x] [`STORY.md`](STORY.md) — normative locked table + tracker

## Out of scope

- Gradle modules / Kotlin (WI-002+)
- Any C-26…C-31 gap closure

## Acceptance

- [x] Implementer can build WI-002/WI-003 without reopening S1 GAPS
- [x] Applicability bound into evaluate (optional preview API); N/A ≠ pass/fail
- [x] No production code required for this WI
