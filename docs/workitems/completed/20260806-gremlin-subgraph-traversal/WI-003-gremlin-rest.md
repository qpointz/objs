# WI-003 — objs-gremlin-service + REST POST /graph/traverse/gremlin

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Matcher REST (closes foundation stages 1–3)  
**Status:** done  
**Depends on:** WI-002

## Goal

Create Gradle leaf module **`:objs-gremlin-service`**, expose Gremlin evaluation over HTTP, and wire it into `:objs-app`.

**Product contract (Explorer parity):** client sends **`matcher`** (anno / anno-expr / chained — same DSL as `POST /graph/query`) + **`script`**. Backend selects induced subgraph1, materializes, runs gremlin-lang, returns `BoMGremlinResult`.

## Scope

- `settings.gradle.kts`: `include(":objs-gremlin-service")`
- Module build: depends on `:objs-gremlin-core` + `:objs-service`
- Packages `org.poc.objs.gremlin.service.*` + Boot autoconfiguration
- `POST /api/v1/objs/graph/traverse/gremlin` (OpenAPI tag **`traverse`**)
- Request body:
  - **`matcher`** required — same JSON DSL as `/graph/query`
  - **`script`** required — gremlin-lang text
  - optional `bindings`, `strategy` (default `envelope`), `traversalOptions`
- Uses `BoMGremlinEngine.selectAndEval(store, matcher, …)`
- `200` + projected result; `400` on bad matcher / script / timeout / unknown language / strategy
- MockMvc: anno, chained, bad matcher, scalar
- `:objs-app` `implementation(project(":objs-gremlin-service"))`

## Out of scope

- Workbench UI (WI-004)
- Changing `/graph/query`
- Putting Gremlin controllers into `:objs-service`
- Implementing non-`gremlin-lang` dialects
- Client-supplied raw `subgraph` body (programmatic `eval(subgraph)` remains for tests)

## Acceptance

- [x] `:objs-gremlin-service` builds; `:objs-app` classpath includes Gremlin REST
- [x] `POST /graph/traverse/gremlin` with `matcher` + `script` returns `BoMGremlinResult`
- [x] Matcher body accepted identically to `/graph/query` (anno / anno-expr / chained)
- [x] MockMvc covers success + 400 paths
- [x] Runnable locally (`:objs-app:run`)
