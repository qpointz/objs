# WI-003 — Optional load + pending deletes

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Load  
**Status:** done  
**Depends on:** WI-002

## Goal

Allow loading an initial subgraph into the draft and track baseline ids so removals become pending deletes for Apply.

## Scope

- Load panel reusing Graph explorer matcher modes (`anno` / `anno-expr` / chained) via `queryGraph`
- Replace draft after confirm; set baseline entity/edge ids
- Clear / Reset to loaded snapshot
- Removing a draft **edge** → pending delete if id in baseline
- Removing a draft **entity** → remove incident draft edges; mark entity and those baseline edge ids as pending deletes
- UI badge/count for pending deletes (not part of Text document)

## Out of scope

- Apply/Validate mutate wiring (WI-006 — completed in same delivery)
- Merge-into-draft load mode

## Acceptance

- [x] Load replaces draft with query result and sets baseline
- [x] Entity delete cascades incident edges in the draft
- [x] Pending delete sets reflect baseline − current correctly
