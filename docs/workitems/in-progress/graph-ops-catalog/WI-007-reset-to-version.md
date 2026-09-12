# WI-007 — Stage D: reset (travel-back) + apply (membership-only)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** D — G-O16a / G-O16b  
**Status:** done  
**Depends on:** WI-006  
**Gaps:** [`GAPS.md`](GAPS.md) § Stage D  

## Goal

Ship two distinct ops:

1. **`resetGraphToVersion`** — full “travel back”: membership + edges + **payloads** from the freeze; optional `truncateAfter`.
2. **`applyGraphVersionMembership`** — “version apply”: membership + edge topology from the freeze; **keep most recent** live entity/edge content (no historical payload overwrite).

REST + Composer actions must not conflate them.

## Deliverables

- [x] Both store APIs + tests
- [x] Separate REST endpoints
- [x] Composer: distinct Travel back vs Apply membership
- [x] Docs/recipes updated; G-O16 marked **resolved**

## Out of scope

- Changing Stages A–C clear/purge/destroy semantics
