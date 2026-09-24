# WI-001 — Perf profile + filler

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Filler  
**Status:** done  
**Depends on:** WI-000  
**Examples:** **AR**

## Goal

Ship Spring profile `perf` with ontology seeds and an idempotent/growable filler that writes noise via batched `NamedGraphStore.mutate`.

## Deliverables

- [x] `application.yml` document for `perf` (`ar.perf.*` + env overlays)
- [x] `PerfDataFiller` / `PerfNoiseFactory` / `PerfFillGate` (+ busy filter) under `org.poc.objs.assetrepository.perf`
- [x] Multi-graph distribution: `graphs`, total `objects`/`edges`, `versions` per graph
- [x] Preassigned UUIDs; batched mutate; progress + gate logging
- [x] CI-safe `@SpringBootTest` with small multi-graph counts

## Out of scope

- Harness script (WI-002)
- Large fills in default CI
