# Story: Foundation after versions

**Slug:** `foundation-after-versions`  
**Branch:** `foundation-after-versions`  
**Status:** closed  
**Folder:** [`docs/workitems/completed/20260822-foundation-after-versions/`](.)  
**Backlog:** [C-19](../../BACKLOG.md) (done)  
**Depends on:** C-18 [`versions-and-snapshots`](../20260819-versions-and-snapshots/STORY.md) (**done**)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

**When:** after C-18 deep graph versions (shipped). Not C-17.

## Goal

Finish store primitives that are **wrong or throwaway** if built on in-place mutate without version pins:

- Reverse lookup that includes **deep graph-version pins**
- Remaining matcher pushdown (FB-3 after C-20 contains/`q`)

C-18 keeps `clone()` as a live deep copy. Snapshot / freeze is `createDeepGraphVersion`. This story does **not** retire clone.

Clocks on HEAD + version rows are **C-18** (WI-002 / WI-003), not this story.

## Stages

| Stage | WIs | Ready | Notes |
|-------|-----|-------|-------|
| 0 — Scaffold | WI-000 | done | Branch + trackers |
| 1 — Design lock | WI-001 | done | Pin reverse + FB-3 remainder; align C-18 ER |
| 2 — Pin reverse | WI-003 | done | Core + SBOM + AR |
| 3 — Matcher ops | WI-004 | done | `>`, prefix pushdown on `obj-expr`; not contains/`q` |
| 4 — Docs | WI-005 | done | Living design sweep |

## Work Items

- [x] WI-000 — Scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — Design lock (`WI-001-design-lock.md`)
- [ ] WI-002 — Timestamps on versions + graph headers (`WI-002-timestamps.md`) — **cancelled; C-18**
- [x] WI-003 — Reverse lookup of pins (`WI-003-pin-reverse.md`)
- [x] WI-004 — FB-3 remaining operators (`WI-004-matcher-ops.md`)
- [x] WI-005 — Living docs (`WI-005-living-docs.md`)

## Out of scope

- C-17 live lookups / `copyGraph` / `mergeGraph` / paging
- C-20 text `q` / contains
- C-18 version store, clocks, and `createDeepGraphVersion`
- D-6 / D-7 product backlog
- Lock/visibility flags
- AR snapshot product (unless pulled in later)

## Acceptance (when implemented)

- `listGraphIdsForEntity` returns live membership **union** graphs that pin any version of the identity
- SBOM asset usage survives live detach when a deep freeze still pins the asset
- `obj-expr` pushdown covers scalar payload `>`, `>=`, `<`, `<=` and prefix (`p.field =~ '^prefix'`) on Postgres; slow path elsewhere
- contains / substring `q` remains **C-20**
- `./gradlew :objs-core:test :sbom-service:test :asset-repository-service:test`
