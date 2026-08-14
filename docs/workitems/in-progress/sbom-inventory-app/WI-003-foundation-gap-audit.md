# WI-003 — Foundation gap audit

**Story:** [`STORY.md`](STORY.md)  
**Gaps:** G-F* / [`FOUNDATION-BACKLOG.md`](FOUNDATION-BACKLOG.md) (`FB-*`)  
**Doc:** [`GRAPH-AND-RETRIEVAL.md`](GRAPH-AND-RETRIEVAL.md)  

## Goal

For every foundation-shaped gap in GRAPH-AND-RETRIEVAL and MI/CDX pressure, update **[`FOUNDATION-BACKLOG.md`](FOUNDATION-BACKLOG.md)** and decide: **example stopgap**, **`in-story` foundation WI**, **`parked`**, or promote to repo backlog. Process lock: do not use `objs-service` REST as a workaround.

**Portfolio MI is a primary pressure test** on objs-core / objs-gremlin-core (G-A5 / G-P12).

### Starting posture (user locks)

| FB | Topic | Posture |
|----|--------|---------|
| FB-1 | Reverse membership | **parked** — confirm stopgap |
| FB-2 | Identity / duplicate query | **parked** — confirm stopgap |
| FB-3 | Searchable pushdown | **open** — slow path OK |
| FB-4 | Gremlin over selection | **open** — prefer in-story or confirm programmatic path already enough |
| FB-5 | Graph-id-set matcher | **open** — prefer **in-story** foundation WI unless deferred with stopgap |

## Deliverables

- [ ] Sync / extend [`FOUNDATION-BACKLOG.md`](FOUNDATION-BACKLOG.md) (`FB-*` rows)  
- [ ] Explicit decision on **FB-4** and **FB-5** (in-story WI vs stopgap)  
- [ ] Audit table: capability → core API today → decision (stopgap / in-story / parked / repo backlog)  
- [ ] If foundation work is in-story: add WI-0xx files + STORY tracker lines (same branch)  
- [ ] If deferred/parked: keep FB row + `GAPS.md` pointer  
- [ ] Update `MILESTONE.md` if foundation WIs are added  

## Out of scope

- Implementing the foundation APIs (that’s the follow-on WIs this audit creates)

## Acceptance

- No Journey 1–3 capability left as “TBD layer”  
- Foundation vs example split is explicit before WI-004 coding starts (update as MI/CDX WIs refine gaps)  
- MI path decision recorded: graph-id-set + Gremlin vs documented stopgap  
