# WI-002 — gremlin-lang engine + result projection

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1–2 — eval + selectAndEval (**closed**)  
**Status:** done  
**Depends on:** WI-001

## Goal

Execute **`gremlin-lang`** scripts against a materialized snapshot with timeout, result projection, and optional traversal options — all in **`:objs-gremlin-core`**. Product path: **matcher → subgraph1 → eval**.

## Scope — Stage 1 (essentials)

- Types in `org.poc.objs.gremlin.core.*` (engine, options, result projection)
- `BoMGremlinTraversalOptions`: optional `timeoutSeconds` (default **60**), optional `language` (default **`gremlin-lang`**; reject unknown)
- `BoMGremlinEngine.eval(subgraph, script, bindings?, strategy?, options?)` with binding `g` = `GraphTraversalSource`
- Evaluate via **`GremlinLangScriptEngine`** / `gremlin-language` (TinkerPop **4.0.0-beta.3**) — **not** Groovy
- Distinguishable parse / timeout / unknown-language errors
- `BoMGremlinResult` per STORY: kind-tagged `items`, **`subgraph` / `views.graph` = subgraph2**, table/scalar views, `primary`, `meta`
- **Subgraph2 projection rules** (STORY): vertices/path vertices → entities; edges/path edges → edges; vertices-only → **induce** edges from subgraph1; edges returned → those only
- Helper `subgraphOrNull()` / equivalent
- Unit tests: `g.V()…` / path / scalar; vertex-only induced subgraph2; timeout override; invalid grammar; unknown `language`; Explorer-shaped field parity

## Scope — Stage 2 (after Stage 1 green; same WI or follow-up commit)

- Facade `selectAndEval(matcher, script, bindings?, strategy?, options?)` using objs-core selection (`BoMGraphStore.selectSubgraph` / equivalent)
- Unit tests: `anno` / `anno-expr` / chained selection before eval
- Unblocks WI-003 Stage 3 matcher REST

## Out of scope

- Implementing alternate materialization strategies beyond `envelope`
- Gremlin-Groovy, SPARQL, GQL dialects (reserve `language` only)
- REST module itself (WI-003) — but Stage 1 `eval` must be ready for WI-003 play REST
- UI (WI-004)
- Persisting Gremlin mutations
- Code in `:objs-core`
- Result pagination / streaming

## Acceptance — Stage 1

- [x] `eval` returns projected results for simple `g.V()…` **gremlin-lang** scripts
- [x] Timeout and parse / unknown-language failures are distinguishable errors
- [x] Default timeout **60s**; `traversalOptions.language` defaults to `gremlin-lang`
- [x] Result envelope covers graph (`subgraph2`), table, scalar, and mixed cases per STORY
- [x] Vertex-only traversal yields induced `subgraph2` from subgraph1 edges
- [x] `subgraph` shape matches Explorer / `BoMSubgraph`
- [x] G-G2 / G-G16 recorded in GAPS

## Acceptance — Stage 2

- [x] `selectAndEval` covers matcher → materialize → eval with Explorer/Composer-equivalent selection
- [x] `anno`, `anno-expr`, and chained matchers each covered before eval (REST MockMvc)
