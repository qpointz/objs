# Story: AR performance profile

**Slug:** `ar-perf-profile`  
**Branch:** `feat/ar-perf-profile`  
**Status:** completed  
**Folder:** [`docs/workitems/completed/20260924-ar-perf-profile/`](.)  
**Backlog:** [D-10](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Follows:** [`20260814-asset-repository-demo-seeds`](../20260814-asset-repository-demo-seeds/STORY.md) (D-4 load kit)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)  
**Closed:** 2026-09-24

## Goal

Add a Spring **`perf`** profile to the asset-repository example that fills configurable **noise** data for performance testing, plus a timed REST harness.

**Normative knobs:**

| Property | Meaning |
|----------|---------|
| `ar.perf.graphs` | Number of collections / named graphs |
| `ar.perf.objects` / `edges` | **Totals** distributed across graphs (edges stay in-graph) |
| `ar.perf.versions` | Deep graph snapshots **per graph** |
| `ar.perf.batch-size` / `collection` | Mutate batch size; name prefix (`perf-noise` or `perf-noise-N`) |

Write path: batched `NamedGraphStore.mutate` with preassigned UUIDs (not domain `writeComposition`). `/api/**` gated with 503 until fill completes. External qsynth/`load.py` remains an optional CSV path.

Operator docs: [`examples/asset-repository/README.md`](../../../examples/asset-repository/README.md) · design: [`docs/design/asset-repository/example.md`](../../../design/asset-repository/example.md)

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Folder + backlog |
| 1 — Filler | WI-001 | done | Profile + bulk filler + CI-safe test |
| 2 — Harness | WI-002 | done | Script + README |
| 3 — Docs | WI-003 | done | Living design / cross-links |

## Work Items

- [x] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [x] WI-001 — Perf profile + filler — examples: **AR** (`WI-001-perf-filler.md`)
- [x] WI-002 — Perf harness + README — examples: **AR** (`WI-002-perf-harness.md`)
- [x] WI-003 — Living docs — examples: **docs** (`WI-003-living-docs.md`)

## Out of scope

- JMH / Gatling / latency SLO gates in CI
- Replacing qsynth / `demo/load-data/load.py`
- Foundation bulk JDBC path
- Combining `demo` + `perf` as the default run
- Cross-graph edges (objs edges are graph-local)

## Acceptance

- [x] `perf` (+ optional `postgres`) fills configured totals across N graphs; versions per graph; growable top-up
- [x] Harness prints observational timings (incl. `edgeCount` in statistics)
- [x] Default `demo` run and `:asset-repository-service:test` green
- [x] `load.py` kit still documented as optional CSV path
- [x] Operator + design docs describe graphs / distribution / versions / gate
