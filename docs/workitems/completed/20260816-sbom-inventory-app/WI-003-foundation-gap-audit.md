# WI-003 — Foundation gap audit

**Story:** [`STORY.md`](STORY.md)  
**Gaps:** G-F* / [`FOUNDATION-BACKLOG.md`](FOUNDATION-BACKLOG.md) (`FB-*`)  
**Doc:** [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md)  
**Status:** complete  

## Goal

Decide foundation vs example for every MI/CDX pressure gap. Do not use `objs-service` REST as a workaround.

## Decisions

| FB | Decision |
|----|----------|
| FB-1 | **parked** — domain scan stopgap |
| FB-2 | **parked** — in-memory duplicate groups stopgap |
| FB-3 | **open** — slow path OK; not in-story |
| FB-4 | **done** — `BoMGremlinEngine.selectAndEval` already sufficient |
| FB-5 | **in-story** — **WI-014** graph-id-set matcher |

## Deliverables

- [x] Sync [`FOUNDATION-BACKLOG.md`](FOUNDATION-BACKLOG.md) + audit table  
- [x] Explicit FB-4 / FB-5 decisions  
- [x] Add [`WI-014-graph-id-set-matcher.md`](WI-014-graph-id-set-matcher.md) + STORY tracker line  
- [x] Update `GAPS.md` foundation rows  
- [x] Note on `MILESTONE.md` (story already in progress)  

## Acceptance

- [x] No Journey 1–3 capability left as TBD layer  
- [x] MI path: R21→R22 → WI-014 matcher → existing Gremlin engine  
