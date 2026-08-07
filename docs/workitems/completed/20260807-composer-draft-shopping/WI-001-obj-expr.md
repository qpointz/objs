# WI-001 — `obj-expr` matcher

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Matcher  
**Status:** done  
**Depends on:** WI-000  
**Gaps:** G-S1–G-S5, G-S19, G-S20

## Goal

Add DSL key **`obj-expr`** for sandboxed JEXL predicates over object `id` / `type` / `schemaVersion`, annotations (`a`), and payload (`p`). Leave **`anno-expr` completely unchanged**.

## Scope

- `BoMObjExprMatcher` (+ compile/bindings/lowerer); register in `BoMMatcherDsl.defaultHandlers`
- **Shared** `BoMAnnoExprEngine`; error codes `MATCHER_OBJ_EXPR_*` (G-S19)
- Context bindings: `id`, `type`, `schemaVersion`, `a`, `p` on candidates
- **Lazy access** like annotations: projection includes payload/annotations when the plan needs them; `LazyJsonMap` deserializes only on touch (G-S5)
- **Pushdown when possible** (G-S4): equality/`&&`/`||` over top-level fields + `a.*` / `p.*` → candidate source; otherwise local JEXL
- Nested payload paths per G-S3 (v1); no collection predicates
- Unit tests: bindings, AND across namespaces, lowerable vs local-eval, blank/too-long, encode/decode, chain with `anno`
- Brief design note in `annotations-and-subgraphs.md` (cross-link next to `anno-expr`; full docs in WI-005)

## Out of scope

- Changing `anno-expr`
- Payload GIN index (pushdown still attempted via `@>`)
- Field-builder UI (G-S18 — users write JEXL)
- Composer / Explorer UI beyond DSL acceptance

## Acceptance

- [x] `POST /graph/query` accepts `{ "obj-expr": "…" }` and filters correctly
- [x] Candidates expose `id`/`type`/`schemaVersion`/`a`/`p` with lazy JSON semantics
- [x] Lowerable expressions use a candidate source; non-lowerable fall back to local eval
- [x] Uses shared `BoMAnnoExprEngine`; `MATCHER_OBJ_EXPR_*` codes
- [x] `anno-expr` tests and behavior unchanged
