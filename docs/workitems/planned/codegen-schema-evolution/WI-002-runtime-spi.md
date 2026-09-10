# WI-002 — Runtime SPI + typed hydrate path

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Runtime  
**Status:** planned (blocked on WI-001 + explicit start)  
**Depends on:** WI-001  
**Examples:** **—** (unit tests in `:objs-api`)

## Goal

Ship foundation contracts and orchestration in `:objs-api`: typed upgrade step SPI, registry/chain helper, hydration policy integration with `TypedGraphView` / binding flow, diagnostics. Maps only at `PayloadMapper` boundaries.

## Deliverables

- [ ] SPI + registry (+ optional in-memory helper per G-E12)
- [ ] Hydrate path: exact bind **or** upgrade-to-latest then Lane A class
- [ ] Failure policy per G-E4
- [ ] Unit tests for chain, missing hop, convert failure
- [ ] No generated migration bodies

## Out of scope

- Lane B export/generator (WI-003)
- Example G-30 fixture (WI-003)
- Persist rewrite API unless G-E6 pulls it in
