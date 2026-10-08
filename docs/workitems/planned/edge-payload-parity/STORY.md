# Story: Edge payload parity

**Slug:** `edge-payload-parity`  
**Branch:** `edge-payload-parity` (scaffold only; no implementation until asked)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/edge-payload-parity/`](.)  
**Backlog:** [C-48](../../BACKLOG.md) (absorbs [C-45](../../BACKLOG.md) edge-property upgrades)  
**Base:** `origin/dev`  
**Depends on:** —  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)  
**Prior trackers:** C-35 [`GAPS.md`](../../completed/20260923-codegen-schema-evolution/GAPS.md) G-E5 (edge-property migrations, deferred)

## Goal

Make edge payload (`Edge.properties`) a first-class citizen with the same reach as entity payload
(`Entity.payload`) across every objs component: model, validation, schema evolution, persistence,
query/matcher, mutation/merge, REST, Gremlin, JGraphT, policies, workbench UI, and examples.

Today edge properties are stored, schema-validated, seeded, deep-versioned, and carried into the
engines, but they **cannot evolve** (rule version pinning, no upgrade-on-read), **cannot be queried
or addressed** (no obj-expr bindings, no GET-by-id, no list/search), have **no annotations**, and are
**under-used** in typed views, UI, and examples. The full inventory is [`GAPS.md`](GAPS.md).

**Not ready to implement** until WI-001 triages the inventory and splits implementation WIs.

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | planned | This folder + gap inventory + backlog row |
| 1 — Design lock | WI-001 | after WI-000 | **Required before code.** Triage G1–G65; add WI-002+ |

## Work Items

- [ ] WI-000 — Story scaffold + gap inventory — examples: **—** (`WI-000-story-scaffold.md`)
- [ ] WI-001 — Design lock: triage gaps, edge schema-version model, WI split — examples: **docs** (`WI-001-design-lock.md`)

## Out of scope

- Any implementation until the user starts this story **and** WI-001 is done
- C-41 freeze-scoped edge history
- Rows marked **(both)** in `GAPS.md` unless WI-001 pulls them in explicitly

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI.  
2. Do not start implementation WIs until WI-001 closes or defers every `open` gap.  
3. Example integration (SBOM + AR) is mandatory per RULES **Concrete example integration**.  
4. Do not close this story until the user asks.
