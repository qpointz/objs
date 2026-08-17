# WI-004 — Living persistence / embedder docs

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Living docs  
**Status:** complete  
**Depends on:** WI-003

## Goal

Point design/embedder docs at the two-Flyway model. RULES.md is already the process SoT (WI-001); this WI updates living design so implementers do not copy the old `classpath:db/migration` merge.

## Deliverables

- [x] [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md) — two history tables, vendor SQL paths, greenfield note (finish any leftover from WI-001)
- [x] [`docs/design/core/README.md`](../../../design/core/README.md) — embedder: depend on objs-core; app Flyway starts at its own `V1`
- [x] Drop any remaining “merge objs into `classpath:db/migration`” wording in design / example READMEs / AGENTS if present
- [x] Example `application.yml` sketches in design match WI-003

## Out of scope

- Public docs site rewrite unless a page currently documents the merged Flyway sequence
- New product features

## Acceptance

- An embedder following only design + RULES.md would configure two Flyway lines correctly
