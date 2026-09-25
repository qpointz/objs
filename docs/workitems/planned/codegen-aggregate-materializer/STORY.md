# Story: Codegen aggregate materializer

**Slug:** `codegen-aggregate-materializer`  
**Branch:** (not started — **planned** only; no implementation until asked)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/codegen-aggregate-materializer/`](.)  
**Backlog:** [C-44](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Depends on:** **C-23** ([`objs-api-codegen`](../../completed/20260828-objs-api-codegen/STORY.md)); prefer **C-43** ([`codegen-write-type-catalog`](../../completed/20260925-codegen-write-type-catalog/STORY.md)) first if both are in flight (catalog makes payload→meta lookup generic).  
**Independent of:** C-20; policy family; C-35 L2 hydrate (except “write latest Lane A” rule).  
**Prior gap:** C-23 **G-19** ([`GAPS.md`](../../completed/20260828-objs-api-codegen/GAPS.md)); design note in [`codegen-and-builder.md`](../../../design/graph/codegen-and-builder.md) § Deferred.  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Consumers:** [`EXAMPLES.md`](EXAMPLES.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Ship an **explicit aggregate materializer** (generated and/or small runtime helper driven by generated relation metadata) that turns a **nested application object graph** — jsonschema2pojo beans with relation collection fields such as `Product.containsDataset` — into a `GraphMutation` (entities + edges), without callers hand-wiring every `addProduct` / `containsComponent` call.

Today:

- Payload DTOs may **carry nested related POJOs** (jsonschema2pojo relation props).
- Generated write path **ignores** those nests for graph edges; callers must use `GraphMutationBuilder` relation methods.
- Design lock (C-23): *“Relations are not serialized as nested entity collections in a mutation.”* Materialization is the **explicit** bridge — not silent Jackson graph dump.

```text
Product(+ nested Component POJOs)
  → materialize(…)
  → GraphMutation { entities.set, edges.set }
  → validateMutate / mutate
```

## Normative (provisional — lock in WI-001)

| Topic | Draft (may change in WI-001) |
|-------|------------------------------|
| Trigger | **Explicit** API (e.g. `GeneratedAggregateMaterializer.materialize(root)` or builder `addAggregate(root)`) — never implicit on `PayloadMapper.toMap` |
| Depth | Recursive walk of generated outbound relations; cycle / shared subgraph policy locked in GAPS |
| Identity | UUID only for node identity (C-23 G-14); optional reuse via C-43 catalog + caller-supplied ids — **not** payload-equality dedupe |
| Edges | Only exact generated allow-list relations; same as builder relation methods |
| Nested relation props on POJO | Materializer reads them to discover children; they are **not** stored as payload fields on the parent `Entity` (strip / ignore on `toEntity`) |
| Output | `GraphMutation` (MERGE default) and/or register into existing `GraphMutationBuilder` |
| Failures | Unknown nest type, disallowed relation, cycle policy violation → fail closed with diagnostics |
| Not this story | HTTP client (G-20); persist cardinality (G-21); L1/L3 schema compaction (C-35 G-X8); auto upsert-by-identity |

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | planned | This folder + backlog |
| 1 — Design lock | WI-001 | after WI-000 | Close open GAPS before code |
| 2 — Runtime / generator | WI-002 | after WI-001 | Emit materializer (+ tests in codegen module) |
| 3 — Consumers | WI-003 | after WI-002 | Codegen examples (± SBOM if locked) |
| 4 — Docs | WI-004 | after WI-003 | Living design + recipes |

## Work Items

- [ ] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [ ] WI-001 — Design lock: walk rules, cycles, strip nests, API shape — examples: **docs** (`WI-001-design-lock.md`)
- [ ] WI-002 — Generate / implement materializer — examples: **—** (`WI-002-materializer.md`)
- [ ] WI-003 — Consumer proof — examples: **codegen (+ locked apps)** (`WI-003-consumers.md`)
- [ ] WI-004 — Living docs — examples: **docs** (`WI-004-living-docs.md`)

## Out of scope

- Implementation until the user starts this story **and** WI-001 is done
- Treating nested POJO graphs as the on-wire `Entity.payload` shape
- Generated REST/HTTP clients (**C-47** / C-23 G-20)
- Kotlin codegen module (**C-46** / C-23 G-6)
- Edge-property / persist rewrite migrations (**C-45** / C-35 G-E5/G-E6)
- Foundation Spring object service

## Acceptance (after implementation)

- [ ] One explicit materialize call builds entities + edges from a nested Product→Component (or locked fixture) graph
- [ ] Parent entity payloads do **not** embed child entities as nested maps after materialization
- [ ] Cycles / shared nodes behave as locked in WI-001
- [ ] Open GAPS resolved or deferred
- [ ] `./gradlew :objs-codegen-java:test` + locked example tests

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI.  
2. Do not start WI-002 until WI-001 is done.  
3. Do not close this story until the user asks.
