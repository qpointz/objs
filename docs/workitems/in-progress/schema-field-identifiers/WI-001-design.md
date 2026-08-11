# WI-001 — Design + story trackers

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 0 — Docs  
**Status:** done  
**Depends on:** —  
**Modules:** docs only (`docs/design`, `docs/workitems`)

## Goal

Record C-14 scope in backlog/milestone and update the object-schema DSL design so implementation WIs have a single normative reference.

## Work

1. **Verify** [`BACKLOG.md`](../../BACKLOG.md) **C-14** and [`MILESTONE.md`](../../MILESTONE.md) (G-13); fix if drifted.
2. Keep [`GAPS.md`](GAPS.md) aligned with STORY locks (G-1…G-3).
3. Update [`docs/design/graph/object-schema-dsl.md`](../../../design/graph/object-schema-dsl.md):
   - Remove OBJECT-node `required` as an authorable/derived DSL field; keep JSON Schema projection rule (derive `"required"` from field flags).
   - Document `identifier` and `searchable` on object fields (placement rules, defaults, `x-objs-*` projection; entity + edge-property schemas).
   - Document identity-map projection (dotted paths), empty-map behavior, create-only / `IDENTIFIER_IMMUTABLE` (including edges), and G-2 version compare rule at a short normative level.
4. No production code in this WI.

## Acceptance

- [x] Design doc matches STORY + GAPS locks
- [x] BACKLOG / MILESTONE / GAPS consistent
- [x] WI tracker in STORY remains accurate
- [x] Story moved to `in-progress/`
