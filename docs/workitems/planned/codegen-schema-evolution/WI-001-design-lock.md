# WI-001 — Design lock

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design lock  
**Status:** planned  
**Depends on:** WI-000  
**Examples:** **docs**

## Goal

Close every **open** row in [`GAPS.md`](GAPS.md) (resolve or defer). Promote the locked decisions from [`DESIGN.md`](DESIGN.md) into living design docs under `docs/design/graph/` (section in `api-and-codegen.md` / `codegen-and-builder.md` or a sibling doc). Do **not** start runtime/codegen code in this WI.

## Deliverables

- [ ] All `open` GAPS closed or explicitly deferred with rationale
- [ ] `DESIGN.md` marked aligned with locked decisions (or superseded by living doc link)
- [ ] Living design doc(s) updated
- [ ] STORY normative table updated to match locks
- [ ] Implementation WIs (WI-002+) adjusted if scope changes

## Out of scope

- `objs-api` / `objs-codegen-java` / example product code
