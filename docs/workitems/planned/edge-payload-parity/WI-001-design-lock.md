# WI-001 — Design lock: triage gaps, edge schema-version model, WI split

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design lock  
**Status:** planned  
**Depends on:** WI-000  
**Examples:** **docs**

## Goal

Turn the [`GAPS.md`](GAPS.md) inventory into an executable plan. No product code in this WI.

## Deliverables

- [ ] Every `open` / `open (both)` row in `GAPS.md` set to `resolved` (in scope, owning WI named),
      `deferred` (with backlog pointer), `cancelled`, or `accepted-risk`
- [ ] Decision on the edge schema-version model (G6/G7): keep per-rule version pin, version range,
      or "any registered version of the rule's schema type"; strict mode or not
- [ ] Decision on edge upgrade-on-read shape (G8/G9): reuse `SchemaUpgradeRegistry`; edge hydrate
      API and `RelationEdgeView` diagnostics
- [ ] Decision on edge query surface (G19–G24): obj-expr edge bindings / DSL key / pushdown scope
- [ ] Decision on edge annotations (G2): in or out of this story
- [ ] Implementation WIs (WI-002+) added to `STORY.md` tracker and stages, each with explicit SBOM /
      AR example-integration obligations
- [ ] Relevant design docs touched only to record decisions (`docs/design/graph/`)

## Out of scope

- Implementation
