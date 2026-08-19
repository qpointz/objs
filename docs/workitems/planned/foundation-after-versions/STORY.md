# Story: Foundation after versions

**Slug:** `foundation-after-versions`  
**Branch:** (not started)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/foundation-after-versions/`](.)  
**Backlog:** [C-19](../../BACKLOG.md)  
**Depends on:** C-18 [`versions-and-snapshots`](../versions-and-snapshots/STORY.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

**When:** only **after** C-18 (versions + snapshot pins). Not C-17.

## Goal

Finish store primitives that are **wrong or throwaway** if built on in-place mutate + pool `clone()`:

- Clocks on **version** rows (not in-place entity/edge columns)
- Reverse lookup that includes **snapshot pins**
- Remaining matcher pushdown (FB-3 after C-20 contains/`q`)
- Retire leftover pool-`clone()` UX if C-18 did not finish it

## Work Items

- [ ] WI-000 — Scaffold (`WI-000-story-scaffold.md`)
- [ ] WI-001 — Design lock (`WI-001-design-lock.md`)
- [ ] WI-002 — Timestamps on versions + graph headers (`WI-002-timestamps.md`)
- [ ] WI-003 — Reverse lookup of pins (`WI-003-pin-reverse.md`)
- [ ] WI-004 — FB-3 remaining operators (`WI-004-matcher-ops.md`)
- [ ] WI-005 — Living docs (`WI-005-living-docs.md`)

## Out of scope

- C-17 live lookups / `copyGraph` / `mergeGraph` / paging
- C-20 text `q` / contains
- C-18 version store and snapshot pin API
- D-6 / D-7 product backlog
- Lock/visibility flags
- AR snapshot product (unless pulled in later)
