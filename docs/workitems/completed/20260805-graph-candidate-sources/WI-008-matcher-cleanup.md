# WI-008 — Matcher package cleanup

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 8 — Cleanup  
**Status:** done  
**Depends on:** WI-002, WI-003, WI-004

## Goal

Inspect matcher and reader types after the source/filter model lands; **delete** what is no longer relevant. No deprecation period, no compatibility shims, no dual code paths.

## Scope

- Inspect `org.poc.objs.core.match` and call sites (reader, store, subgraph selector, tests, docs).
- **Delete** obsolete types such as `BoMPushableMatcher`, `BoMNonPushableMatcher`, dead adapters, unused expression/routing paths, stale test names asserting the old taxonomy.
- Remove `is BoMPushableMatcher` (and similar) branches.
- Rewrite every in-repo caller in this story.
- Align design docs (`annotations-and-subgraphs`, `persistence`, `model`, core README) to source/filter terminology.

## Out of scope

- Soft deprecation / `@Deprecated` retention
- External binary compatibility for removed types
- API pagination / result-size caps / sparse HTTP projection (compensating follow-up; see STORY)
- Full benchmark refresh (WI-009)

## Acceptance

- [x] No remaining pushable/non-pushable hierarchy in production code
- [x] No dual execution paths for the same DSL
- [x] Tests and references updated; compile clean
- [x] Design docs describe candidate source / filter (not pushable taxonomy)
