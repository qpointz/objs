# WI-004 — Matcher `subg-expr` (+ optional `subgraph` id) 

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — REST + matcher  
**Status:** done  
**Depends on:** WI-003  
**Modules:** `:objs-core` (matcher + selection), `:objs-service` (query tests); UI mode wired in WI-005

## Goal

Ship pack discovery for **Explorer / Composer UI** via **`subg-expr`**, and keep **get-by-id** as the programmatic path (already WI-002/003 REST). Optional matcher sugar `subgraph: { id }` for query handoff when an app holds a ref (G-S7).

## Programmatic get-by-id (must already exist from WI-002/003)

Not re-implemented here — verify parity:

- Domain: `get(id) → BoMResolvedSubgraph`
- REST: `GET /api/v1/objs/graph/subgraphs/{id}`
- Use case: app object stores `subgraphId` → open pack without an expression

## Matcher DSL

### `subg-expr` (required for UI)

```yaml
subg-expr: "a.decisionId == 'D-42'"
```

```yaml
- subg-expr: "a.env == 'prod'"
- obj-expr: "type == 'Policy'"
```

Behaviour:

1. Evaluate expression against each `bom_subgraph` header (`id`, `a` = annotations map; same JEXL sandbox family as `obj-expr`).
2. Entity candidates = **union** of member entity ids of matching packs (dedupe).
3. Edges = **union** of **stored** membership edges for those packs (dedupe; **do not** re-induce).
4. Later chain stages filter entities as today.
5. Zero matching packs → empty subgraph (success), unless expression compile/eval fails → 400.

### Optional sugar `subgraph: { id }`

```json
{ "subgraph": { "id": "…" } }
```

Same result as get-by-id members; unknown id → **400** with issue (explicit ref failed).

## Implementation hints

- Register handlers in `BoMMatcherDsl` next to `obj-expr` / `ids`.
- Reuse shared JEXL setup from `BoMObjExprMatcher` where possible; bind `id`, `a` only (no `p`/`type` on packs unless you add payload later — packs have no payload column).
- Source-capable stage-0 preferred; may scan all pack headers for v1 if count is small (document; optimize later).
- Member load: reuse store resolve / membership repos — never fall back to induce-among-ids for edges.

## Tests

- `subg-expr` matches one pack → members equal get-by-id
- `subg-expr` matches two packs → union members
- Chain `subg-expr` then `obj-expr` filters entities
- Bad expression → 400
- `subgraph: { id }` parity with GET; unknown id → 400

## Out of scope

- Platform annotation vocabulary (G-S11)
- Full MatcherQueryForm polish (WI-005 adds UI mode)
- Changing `ids` / `anno` induce behaviour

## UI note (WI-005)

Explorer/Composer: **`subg-expr` mode** in shared matcher form to find packs; open uses **get-by-id** + replace draft (G-S3).

## Implementation checklist

- [ ] `subg-expr` matcher + stored-edge member projection
- [ ] Optional `subgraph: { id }` sugar
- [ ] Confirm GET-by-id still the programmatic API
- [ ] Tests green; STORY `[x]`; commit; push

## Acceptance

- [ ] UI-oriented `subg-expr` returns correct member unions
- [ ] Chains with object-level filters work
- [ ] Get-by-id (domain + REST) remains the non-expression open path
- [ ] No extra induced edges beyond membership

## Commit message hint

`[feat] Add subg-expr matcher and subgraph id sugar (WI-004)`
