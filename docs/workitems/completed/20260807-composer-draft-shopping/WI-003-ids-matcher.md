# WI-003 — `ids` matcher for edge refresh

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Id-set query  
**Status:** done  
**Depends on:** WI-000  
**Gaps:** G-S6, G-S20

## Goal

Add DSL key **`ids`** so Composer can refresh **induced edges** among a set of store entity ids without building huge `obj-expr` OR-chains.

## Scope

- Matcher `{ "ids": ["uuid", …] }` → select those entities + induced edges (source ∈ set ∧ target ∈ set)
- Source-capable: SQL/`IN` (or equivalent) on entity id list; empty list → empty subgraph
- Invalid UUID → **400** with clear issue code (G-S6)
- Participates in matcher chains; source-capable as stage 0 (G-S20)
- Unit + controller/MockMvc tests
- Brief design note under matcher DSL (full docs in WI-005)

## Out of scope

- Composer UI (wired in WI-004)
- Changing induction rules

## Acceptance

- [x] `/graph/query` with `ids` returns entities + induced edges only among those ids
- [x] Empty `ids` → empty subgraph; invalid UUID → 400
