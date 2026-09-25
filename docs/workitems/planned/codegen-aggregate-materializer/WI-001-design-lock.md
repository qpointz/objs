# WI-001 — Design lock

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design lock  
**Status:** planned  
**Depends on:** WI-000  
**Examples:** **docs**

## Goal

Close every **open** row in [`GAPS.md`](GAPS.md) (G-M1…G-M9). Until this WI is `[x]`, do not implement WI-002.

## Docs (this WI)

- [ ] Resolve G-M1…G-M9 (API, which nests, cycles, strip, edge props, direction, depth, C-43 tie-in, consumers)
- [ ] Update [`STORY.md`](STORY.md) **Normative** to match locks
- [ ] [`EXAMPLES.md`](EXAMPLES.md) — required modules for WI-003
- [ ] Contract notes in [`codegen-and-builder.md`](../../../design/graph/codegen-and-builder.md) / [`api-and-codegen.md`](../../../design/graph/api-and-codegen.md) (full rewrite WI-004)
- [ ] Confirm C-43 ordering: hard depends / soft prefer / independent

## Out of scope

- Generator / runtime code (WI-002+)
- HTTP client, Kotlin codegen, C-35 persist migrations (G-X*)

## Acceptance

- An embedder could implement the materializer without reopening G-M1…G-M9
- Explicit-only boundary (G-M10) remains non-negotiable
