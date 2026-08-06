# Gaps — gremlin-subgraph-traversal

Decisions locked in [`STORY.md`](STORY.md). Remaining / half-open items for this story or follow-ups.

| ID | Topic | Status | Notes |
|----|-------|--------|-------|
| G-G1 | TinkerPop version pin | **resolved** | **`4.0.0-beta.3`** — published line with first-class JDK **21** CI (min 17). Accept pre-release; revisit on `4.0.0` GA. Reject `3.8.1` / `3.7.x` for this toolchain (no official JDK 21). |
| G-G2 | Traversal options / timeout | **resolved** | Optional `BoMGremlinTraversalOptions`: `timeoutSeconds` (default **60**), reserved `language` (default **`gremlin-lang`**). Programmatic + REST. Unknown language / ≤0 timeout → error. Not Spring-property-only. |
| G-G3 | Sandbox strength | resolved (v1) | **`gremlin-lang`** grammar limits + eval timeout; no Groovy sandbox required. (Groovy out of scope.) |
| G-G4 | Result size caps | deferred | No hard pagination/caps in this story; backlog follow-up if needed |
| G-G5 | UI path/graph viz | resolved (v1) | Query UI: Structured tab uses Explorer-like canvas on `subgraph2` when present; else table/scalar; Raw JSON. Structured UX is tactical/demo-grade. |
| G-G6 | Write-back from Gremlin | resolved (v1) | Read-only snapshot; mutations never persist to `BoMGraphStore` |
| G-G7 | Matcher on REST | **resolved** | Traverse REST requires **`matcher`** (Explorer DSL) + `script`. Backend runs `selectSubgraph` then gremlin-lang. Empty selected subgraph → empty TinkerGraph; script may still run. |
| G-G8 | Binding name for traversal | resolved | Script binding `g` = `GraphTraversalSource`; optional `graph` = underlying `Graph` |
| G-G9 | Selection parity | resolved | Same matcher DSL and induced-subgraph semantics as Explorer query and Composer draft load; no matcher subset |
| G-G10 | Hierarchical materialization | resolved (v1) | Strategy hook; implement **`envelope`** only; document `flatten` / `nested-vertices` as future |
| G-G11 | REST/API strategy param | **resolved** | Optional body `strategy` string, default **`envelope`**; unknown → `400` |
| G-G12 | Module split | resolved | Gremlin lives in `:objs-gremlin-core` + `:objs-gremlin-service`; not inside foundation core/service |
| G-G13 | Result representation | resolved | `BoMGremlinResult`: items + `subgraph`/`views.graph` + table/scalar + `primary` + `meta` |
| G-G14 | Pipeline / Explorer parity | resolved | `matcher → subgraph1 → traversal → subgraph2` when projectable; Gremlin internal; **subgraph optional** — absent for table/scalar/non-graph results (success, not error) |
| G-G15 | Subgraph2 edge induction | resolved | Vertices-only hits → induce edges from subgraph1 among those ids; if edges returned → use those only |
| G-G16 | Script input language | **resolved (v1)** | **`gremlin-lang` only** this story. `traversalOptions.language` reserved; reject unknown. Groovy out of scope. Apache **sparql-gremlin** removed in TinkerPop 4 — backlog only (not on our pin); future dialect candidate: **`gql`** / TinkerGQL. |
| G-G17 | SBOM / concrete-example integration | **resolved (skip code)** | No ontology or `:objs-sbom-example` code change — traverse is generic over any stored graph. Smoke: `:objs-app` + `sbom` profile seeds + Query UI / traverse REST (documented in `docs/design/graph/gremlin.md`, `docs/design/sbom/example.md`). Engine/REST covered by `:objs-gremlin-*` tests. |
