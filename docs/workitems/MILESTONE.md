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
- [`gremlin-subgraph-traversal`](completed/20260806-gremlin-subgraph-traversal/STORY.md) — Matcher → subgraph1 → envelope TinkerGraph → gremlin-lang → `BoMGremlinResult`; `:objs-gremlin-core` / `:objs-gremlin-service`; Query workbench (`/workbench/query`); TinkerPop `4.0.0-beta.3`; see also [`docs/design/graph/gremlin-examples.md`](../design/graph/gremlin-examples.md) (C-9).
- [`json-schema-generation`](completed/20260807-json-schema-generation/STORY.md) — Configurable full-catalog JSON Schema export (`BoMJsonSchemaExportOptions`: dialect, includeEdges none/outbound/linked, edge-property `$defs`); Schemas overview UI options (C-10).
- [`composer-draft-shopping`](completed/20260807-composer-draft-shopping/STORY.md) — Composer Add objects (side pane); `obj-expr` + `ids` matchers; draft merge/exclude; shared matcher UI + visual chain builder; Explorer handoff auto-merge (C-11).
- [`subgraphs-materialization`](completed/20260807-subgraphs-materialization/STORY.md) — Soft-link subgraph packs (`bom_subgraph` M2M); `subg-expr` / id matchers; hard snapshot clone; Composer Save ▾ Subgraph/Snapshot; Explorer canvas handoff (C-12).
- [`graphs-from-objects`](completed/20260811-graphs-from-objects/STORY.md) — Global `bom_entity` pool + many `bom_graph`s (no global graph); multi-graph membership; graph-local edges; matchers `all` / `graph-expr` / `obj-expr` / chained; REST `/graphs` + `/entities`; workbench Open/New graph; SBOM on graphs; pack-era cleanup; `gql-gremlin` (C-13).
- [`workbench-chrome-regroup`](completed/20260811-workbench-chrome-regroup/STORY.md) — Explorer Graph vs Selection modes; Explore-scope; Open-graph search; Composer L2 Save/Snapshot + edit migrate; Query L2 Exec; schema `usage` scalar (U-4).
- [`schema-field-identifiers`](completed/20260811-schema-field-identifiers/STORY.md) — Drop OBJECT-level `required` list; field `identifier` + `searchable`; identity projection + create-only immutability; graph-header annotation pushdown (C-14).
- [`objects-shelf`](completed/20260813-objects-shelf/STORY.md) — Objects top-level view (pool search + shelf) → New graph in Composer (U-5).
- [`build-system-cleanup`](completed/20260813-build-system-cleanup/STORY.md) — Gradle: drop Spring DM for `platform()`, prune deps/catalog, minimize plugins (P-1).
- [`ui-gradle-node`](completed/20260813-ui-gradle-node/STORY.md) — Workbench SPA via `com.github.node-gradle.node` 7.0.2 + `:objs-service-ui` (U-6).
- [`schema-migration-docs`](completed/20260813-schema-migration-docs/STORY.md) — Seed-format implementer reference + JSON Schema → YAML seeds guide + Python nano-framework; workbench missing-UI 503 (C-15).
- [`asset-repository-example`](completed/20260814-asset-repository-example/STORY.md) — Centralized asset object store: collections, domain REST, Mantine UI, schema catalog, Python client (D-3).
- [`asset-repository-demo-seeds`](completed/20260814-asset-repository-demo-seeds/STORY.md) — Extensible seed kinds, AI catalog demo volumes, qsynth load-data, collection query exec stats (D-4).
- [`asset-repository-ops-fixes`](completed/20260814-asset-repository-ops-fixes/STORY.md) — Postgres collection search casts, SPA deep-link filters, batched `load.py` (D-5).
- [`sbom-inventory-app`](completed/20260816-sbom-inventory-app/STORY.md) — Applications \| Portfolios inventory app; portfolio-scoped MI (latest-version graphs + Gremlin); weak CDX; programmatic objs (D-2). WI-000…WI-015.
- [`spa-url-classpath-align`](completed/20260817-spa-url-classpath-align/STORY.md) — Align SPA URL prefix with classpath folder (`/workbench`, `/ar`, `/sbom`); rename `:objs-app` → `:objs-service-app`; UI JAR on the runner only (P-2). WI-000…WI-004.
- [`multi-bom-app-versions`](completed/20260817-multi-bom-app-versions/STORY.md) — Multi-BOM constituents + Combined SBOM; multi-draft (target + optional combine); tags; fingerprint name/category; inventory UI polish (D-8). WI-000…WI-008.
- [`flyway-module-isolation`](completed/20260817-flyway-module-isolation/STORY.md) — Two Flyway lines: objs-core vendor SQL + `flyway_schema_history_objs` before Boot Flyway; examples as derived apps (P-3). WI-000…WI-004.
- [`source-export`](completed/20260828-source-export/STORY.md) — Makefile-driven clean source export with package/module transformation, SPI rewriting, verification, and selected documentation (P-4). WI-000…WI-004.
- [`objs-api-codegen`](completed/20260828-objs-api-codegen/STORY.md) — Spring-free Kotlin/JVM API, reusable Java code generator, schema-aware mutation builder, typed in-memory read view, and consumer integration (C-23). WI-000…WI-009.
- [`catalog-schema-metadata`](completed/20260818-catalog-schema-metadata/STORY.md) — Allowed-edge description/verbs; free-text STRING `format`; tags + string attributes; enum `caption`; example schema browse lists allow-list rules (C-16). WI-000…WI-006.
- [`live-store-apis`](completed/20260819-live-store-apis/STORY.md) — Live store APIs before versions: catalog helpers, reverse lookup, identity query, `copyGraph` + `mergeGraph`, paging (C-17). WI-000…WI-007.
- [`versions-and-snapshots`](completed/20260819-versions-and-snapshots/STORY.md) — HEAD+history, clocks, `createDeepGraphVersion` freeze (same graph); `clone()` kept as new-id copy; workbench versions + product tour; SBOM fingerprint freeze (C-18). WI-000…WI-007.
- [`workbench-ux`](completed/20260822-workbench-ux/STORY.md) — Shared graph context (Explorer/Objects/Query); graph version pin; Notes 3–9 view chrome; object viewer; Query/Objects/Composer/Schema polish; product tour v2 + `ui.md` (U-7). WI-000…WI-012, WI-005.
- [`foundation-after-versions`](completed/20260822-foundation-after-versions/STORY.md) — Pin reverse lookup (live ∪ version pins); leftover matcher pushdown `>` / prefix (C-19). WI-000…WI-005 (WI-002 cancelled → C-18).
- [`catalog-cache-ttl`](completed/20260826-catalog-cache-ttl/STORY.md) — Write-through + Caffeine TTL catalog snapshots; `POST /registry/refresh`; `objs.catalogs.cache-ttl` (C-21). WI-001…WI-003.
- [`graph-mutate-replace`](completed/20260826-graph-mutate-replace/STORY.md) — Kind-first mutate body (`set`/`unset`); named-graph MERGE (`PATCH`) vs REPLACE (`PUT`); `bomMutation` builder; Composer Save/Overwrite; SBOM `replaceBom` (C-22). WI-000…WI-008.
- [`graph-frontend-jgrapht`](completed/20260903-graph-frontend-jgrapht/STORY.md) — Shared `GraphFragment` / policy normalization; `:objs-jgrapht-core` / `:objs-jgrapht-service`; workbench cycle analysis. WI-000…WI-005.
- [`workbench-cosmetic`](completed/20260903-workbench-cosmetic/STORY.md) — Workbench cosmetic polish: Objects load splash; object inspect live Graphs; open-graph dialog polish (U-8). WI-000…WI-002.
- [`objs-core-spring-split`](completed/20260903-objs-core-spring-split/STORY.md) — Spring-free `:objs-persistence` + `:objs-autoconfigure`; expand `:objs-api`; Gradle rename G-X7 (C-25). WI-000…WI-007.

### Planned

- [`store-text-search`](planned/store-text-search/STORY.md) — FB-3 contains/`q`; design first (C-20). Does not block C-18.
- [`objs-policy`](planned/objs-policy/STORY.md) — Foundation policy + **suites** (M:N, hierarchy, folder roll-up); applicability SPI; Drools-first; design first (C-24).

