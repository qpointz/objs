# WI-007 — Shared Open-graph search (API + modal)

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3b — Open graph  
**Status:** done  
**Depends on:** WI-001  
**Modules:** `:objs-core`, `:objs-service`, UI

## Goal

Replace full `listGraphs()` Open-graph UX with **one shared dialog** used by Explorer / Composer / Query.

Ship an **extensible** search contract (G-U10): clients depend on a small stable shape; backend can later swap in FTS/ranking **without** breaking that shape. **Do not** implement FTS now — v1 simple match is enough.

## API contract (normative — G-U10)

```http
GET /api/v1/objs/graphs/search?q={text}&limit={n}&expr={graph-expr}
```

| Rule | Detail |
|------|--------|
| Empty | No `q` and no `expr` → `{ "items": [] }` — never dump all graphs |
| `limit` | UI uses ≤15; server caps reasonably |
| v1 match | Id / UUID-prefix + case-insensitive substring on id + annotation key/value; `q` ∧ `expr` if both set |
| Response | `{ "items": [ { "id", "annotations" } ] }` |
| Extend later | Additive query params + additive JSON fields only; UI ignores unknowns |
| FTS | Out of this WI (G-U11); same path |

Backend implementation may be whatever is simplest (e.g. filter over graph headers) — not part of the public contract.

## UI

Rewrite [`OpenGraphModal.tsx`](../../../../objs-service/ui/src/OpenGraphModal.tsx); all call sites use the same component. Debounced `q`; optional expr expander.

## Acceptance

- [x] Shared modal on Explorer / Composer / Query
- [x] Debounced search; ≤15 results; empty query → empty list
- [x] API matches G-U10 contract; documented as additive-extendable; no FTS
- [x] Tests for API + modal smoke
