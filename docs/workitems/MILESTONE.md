# Milestones

**Draft release notes.** Treat this file as the **working draft** of **`releases/RELEASE-x.y.z.md`**
for the **next** version only. See [`RULES.md`](RULES.md) § **Milestone ledger (`MILESTONE.md`)** and
§ **Release (version) process**.

## 0.1.0

**Target date:** TBD — **not released.**

### Completed

- Project scaffold — Gradle multi-module shell (`objs-core`, `objs-service`), workitem process docs.
- [`entity-graph-foundation`](completed/20260728-entity-graph-foundation/STORY.md) — Kotlin entity store: `BoMEntity`/`BoMEdge`, annotations/subgraphs, schema + allow-list (`*` wildcards), persist gate, Flyway/JPA (H2), packages `org.poc.objs`; modules flattened; `:objs-app` runnable assembly.
- [`entity-rest-api`](completed/20260728-entity-rest-api/STORY.md) — `/graph` + `/registry` REST, springdoc OpenAPI 3.0.3, MockMvc tests; `BoM*` rename; catalogs in-memory until C-3.
- [`sbom-typed-example`](completed/20260728-sbom-typed-example/STORY.md) — typed toolkit; full canonical ontology (A–D); `/api/v1/example/sbom`; Python bulk seed; graph explorer SPA at `/ui/`.
- [`graph-config-seeds`](completed/20260729-graph-config-seeds/STORY.md) — PostgreSQL-authoritative catalogs; object-schema DSL and authoring workbench; lazy/pushable graph reads; flat multi-document seed import/export with durable startup ledger; canonical SBOM YAML.
- [`matcher-query-language`](completed/20260729-matcher-query-language/STORY.md) — extensible JSON/YAML matcher DSL (`anno`, sandboxed JEXL `anno-expr`, ordered chains); first-stage PostgreSQL pushdown; sole `POST /api/v1/objs/graph/query`; graph explorer matcher modes.
- [`allowed-edge-cardinality`](completed/20260803-allowed-edge-cardinality/STORY.md) — allow-list cardinality `UNSPECIFIED` / `1:1` / `1:*` (domain, Flyway, seeds, REST, schema UI).
- [`schema-workbench-unify`](completed/20260803-schema-workbench-unify/STORY.md) — unify Schemas workbench; top nav; object-level edge CRUD (draft until save); Visual/Schema/Expert/JSON Schema tabs.
- [`schemas-catalog-overview`](completed/20260803-schemas-catalog-overview/STORY.md) — Schemas entry overview: full ontology graph (dagre) + catalog seed import/export.
- [`registry-graph-io-formats`](completed/20260805-registry-graph-io-formats/STORY.md) — Split ontology vs graph import/export under `/registry` and `/graph` with `format=`; seeds now; full-catalog JSON Schema export.
- [`object-linter-visual`](completed/20260805-object-linter-visual/STORY.md) — Object linter Visual/Text draft workspace; matcher load; schema forms; graph mutate (upsert + delete) Validate/Apply; workbench routes and schema polish.
- [`graph-candidate-sources`](completed/20260805-graph-candidate-sources/STORY.md) — Workbench SPA in `:objs-service`; candidate-source query plan (`anno` / lowerable `anno-expr` incl. OR); bound edges; lazy candidate JSON; Postgres JSONB + GIN; Explorer/Composer exec stats and `qid` selection history (DSL frozen; no API pagination/caps).
- [`gremlin-subgraph-traversal`](completed/20260806-gremlin-subgraph-traversal/STORY.md) — Matcher → subgraph1 → envelope TinkerGraph → gremlin-lang → `BoMGremlinResult`; `:objs-gremlin-core` / `:objs-gremlin-service`; Query workbench (`/workbench/query`); TinkerPop `4.0.0-beta.3` (C-9).

### In Progress

_(none)_

### Planned

_(none)_
