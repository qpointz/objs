# Completed stories (archive)

Closed stories are **moved** here from `planned/` or `in-progress/` — they are **not** deleted.

## Naming

```text
docs/workitems/completed/YYYYMMDD-<story-slug>/
```

## Index (optional, newest first)

- [`20260924-graph-entity-edge-vocab`](20260924-graph-entity-edge-vocab/STORY.md) — Freeze SQL + REST entity/edge vocabulary; graph `/edges` CRUD (C-40)
- [`20260924-ar-perf-profile`](20260924-ar-perf-profile/STORY.md) — AR `perf` multi-graph noise fill + timed harness (D-10)
- [`20260923-sbom-ar-openapi`](20260923-sbom-ar-openapi/STORY.md) — SBOM + AR domain OpenAPI / Swagger UI; demo portfolios; Flat/By category (D-9)
- [`20260923-codegen-schema-evolution`](20260923-codegen-schema-evolution/STORY.md) — L2 schema upgrade SPI + TypedGraphView hydrate (C-35)
- [`20260915-policy-consumer`](20260915-policy-consumer/STORY.md) — C-30 superseded (C-31 REST + SBOM assessment)
- [`20260914-api-store-improvements`](20260914-api-store-improvements/STORY.md) — `NamedGraphStore.exists` + structured `ValidationIssue` (C-38)
- [`20260912-policy-batch`](20260912-policy-batch/STORY.md) — Thin policy batch / sequential executor (C-29)
- [`20260912-export-package-sensitive-assets`](20260912-export-package-sensitive-assets/STORY.md) — Export `.drl` rewrite + codegen foundation-dir path guard (P-5)
- [`20260912-transaction-recipes`](20260912-transaction-recipes/STORY.md) — Spring integration how-to + transaction recipes (C-37)
- [`20260912-graph-ops-catalog`](20260912-graph-ops-catalog/STORY.md) — Graph lifecycle clear/purge/destroy/compact, backdated freeze, reset/apply, recipes (C-36)
- [`20260911-policy-status-severity-vocab`](20260911-policy-status-severity-vocab/STORY.md) — Status vs finding severity vocab; SuiteStrategy pack; evaluation trees (C-34)
- [`20260903-objs-core-spring-split`](20260903-objs-core-spring-split/STORY.md) — Spring-free persistence + autoconfigure; expand objs-api; rename objs-persistence (C-25)
- [`20260903-workbench-cosmetic`](20260903-workbench-cosmetic/STORY.md) — Workbench cosmetic polish: Objects splash, inspect Graphs, open-graph dialog (U-8)
- [`20260903-graph-frontend-jgrapht`](20260903-graph-frontend-jgrapht/STORY.md) — Graph fragments, JGraphT analysis, workbench cycles
- [`20260828-objs-api-codegen`](20260828-objs-api-codegen/STORY.md) — Spring-free API and schema-driven Java graph codegen (C-23)
- [`20260828-source-export`](20260828-source-export/STORY.md) — Makefile source export with package/module transformation and verification (P-4)
- [`20260826-catalog-cache-ttl`](20260826-catalog-cache-ttl/STORY.md) — Catalog cache TTL (Caffeine) + registry refresh (C-21)
- [`20260822-foundation-after-versions`](20260822-foundation-after-versions/STORY.md) — Pin reverse lookup; leftover matcher pushdown `>` / prefix (C-19)
- [`20260822-workbench-ux`](20260822-workbench-ux/STORY.md) — Shared graph context; version pin; view chrome; object viewer; tour v2 (U-7)
- [`20260819-versions-and-snapshots`](20260819-versions-and-snapshots/STORY.md) — HEAD+history, clocks, deep graph freeze, workbench versions + tour, SBOM fingerprint freeze (C-18)
- [`20260819-live-store-apis`](20260819-live-store-apis/STORY.md) — Live store APIs: catalog, reverse, identity, `copyGraph`/`mergeGraph`, paging (C-17)
- [`20260818-catalog-schema-metadata`](20260818-catalog-schema-metadata/STORY.md) — Catalog metadata: edge verbs, tags/attributes, enum captions (C-16)
- [`20260817-flyway-module-isolation`](20260817-flyway-module-isolation/STORY.md) — Isolate objs Flyway from derived-app Flyway (P-3)
- [`20260817-multi-bom-app-versions`](20260817-multi-bom-app-versions/STORY.md) — Multi-BOM versions, Combined SBOM, parallel drafts, fingerprints (D-8)
- [`20260817-spa-url-classpath-align`](20260817-spa-url-classpath-align/STORY.md) — SPA URL = classpath folder; `:objs-service-app` workbench runner (P-2)
- [`20260816-sbom-inventory-app`](20260816-sbom-inventory-app/STORY.md) — Applications \| Portfolios inventory app; portfolio MI; weak CDX (D-2)
- [`20260814-asset-repository-ops-fixes`](20260814-asset-repository-ops-fixes/STORY.md) — Postgres collection search, SPA refresh filters, batched load.py (D-5)
- [`20260814-asset-repository-demo-seeds`](20260814-asset-repository-demo-seeds/STORY.md) — Extensible seed kinds, AI catalog demo volumes, qsynth load-data, query exec stats (D-4)
- [`20260814-asset-repository-example`](20260814-asset-repository-example/STORY.md) — Asset repository example: collections object store, domain REST + UI (D-3)
- [`20260813-schema-migration-docs`](20260813-schema-migration-docs/STORY.md) — Seed format + JSON Schema → YAML seeds guide + Python nano-framework (C-15)
- [`20260813-ui-gradle-node`](20260813-ui-gradle-node/STORY.md) — Workbench SPA via node-gradle + `:objs-service-ui` (U-6)
- [`20260813-objects-shelf`](20260813-objects-shelf/STORY.md) — Objects view + shelf → Composer New graph (U-5)
- [`20260813-build-system-cleanup`](20260813-build-system-cleanup/STORY.md) — Gradle `platform()` BOM, prune deps, minimize plugins (P-1)
- [`20260811-schema-field-identifiers`](20260811-schema-field-identifiers/STORY.md) — Field `identifier`/`searchable`; drop OBJECT `required` list; identity immutability; graph-header pushdown (C-14)
- [`20260811-workbench-chrome-regroup`](20260811-workbench-chrome-regroup/STORY.md) — Explorer Graph/Selection chrome; Explore-scope; Open-graph search; Composer Save/Snapshot; Query L2 (U-4)
- [`20260811-graphs-from-objects`](20260811-graphs-from-objects/STORY.md) — Global entity pool + many graphs; graph-local edges; `all`/`graph-expr`/`obj-expr` (C-13)
- [`20260807-subgraphs-materialization`](20260807-subgraphs-materialization/STORY.md) — Soft-link subgraph packs + snapshot + Composer Save/Subgraph/Snapshot (C-12)
- [`20260807-composer-draft-shopping`](20260807-composer-draft-shopping/STORY.md) — Composer Add objects; `obj-expr` / `ids`; visual chain matcher (C-11)
- [`20260807-json-schema-generation`](20260807-json-schema-generation/STORY.md) — Configurable full-catalog JSON Schema export (C-10)
- [`20260806-gremlin-subgraph-traversal`](20260806-gremlin-subgraph-traversal/STORY.md) — Matcher → Gremlin traverse + Query UI (C-9)
- [`20260805-graph-candidate-sources`](20260805-graph-candidate-sources/STORY.md) — Candidate-source query plan, JSONB/GIN, workbench in objs-service
- [`20260805-object-linter-visual`](20260805-object-linter-visual/STORY.md) — Object linter Visual/Text draft workspace + graph mutate
- [`20260805-registry-graph-io-formats`](20260805-registry-graph-io-formats/STORY.md) — Registry/graph multi-format I/O + full-catalog JSON Schema
- [`20260728-entity-rest-api`](20260728-entity-rest-api/STORY.md) — `/graph` + `/registry` REST, OpenAPI, `BoM*` types
- [`20260728-entity-graph-foundation`](20260728-entity-graph-foundation/STORY.md) — Kotlin entity store foundation (`BoMEntity`/`BoMEdge`, validation, Flyway/JPA, `objs-app`)
