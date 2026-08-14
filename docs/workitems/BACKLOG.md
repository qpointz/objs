# Backlog

Internal tracker for planned product items. **Open** work uses **`backlog`**, **`planned`**, or
**`in-progress`**. Shipped work is recorded under [`releases/`](releases/); **`done`** rows are
**pruned at release housekeeping** — see [`RULES.md`](RULES.md) § **Release (version) process**.

**Legend:**
- **Status**: `backlog` | `planned` | `in-progress` | `done`
- **Type**: feature | improvement | fix | refactoring | test | docs
- **Source**: design document (relative to `docs/design/`) or work item (`docs/workitems/`)

---

## core — Entity / graph core

| # | Item | Type | Status | Source |
|---|------|------|--------|--------|
| C-1 | Entity graph foundation (domain, subgraph, validation, persistence) | feature | done | [`completed/20260728-entity-graph-foundation/`](completed/20260728-entity-graph-foundation/STORY.md) |
| C-2 | REST API for entities / edges / subgraphs + registry | feature | done | [`completed/20260728-entity-rest-api/`](completed/20260728-entity-rest-api/STORY.md) |
| C-3 | Persist central schema catalog (type+version) + allowed-edge rules as PostgreSQL tables | feature | done | [`completed/20260729-graph-config-seeds/`](completed/20260729-graph-config-seeds/STORY.md) Stage 1 |
| C-4 | Extensible multi-document graph configuration seeds with durable ledger | feature | done | [`completed/20260729-graph-config-seeds/`](completed/20260729-graph-config-seeds/STORY.md) Stage 2 |
| C-5 | Extensible JSON/YAML matcher DSL with chained annotation and JEXL predicates | feature | done | [`completed/20260729-matcher-query-language/`](completed/20260729-matcher-query-language/STORY.md) |
| C-6 | Allowed-edge cardinality (`UNSPECIFIED` / `1:1` / `1:*`) on edge schema + UI | feature | done | [`completed/20260803-allowed-edge-cardinality/`](completed/20260803-allowed-edge-cardinality/STORY.md) |
| C-7 | Split registry/graph multi-format import/export + full-catalog JSON Schema | feature | done | [`completed/20260805-registry-graph-io-formats/`](completed/20260805-registry-graph-io-formats/STORY.md) |
| C-8 | Graph query backend performance (+ workbench UI into objs-service prerequisite) | improvement | done | [`completed/20260805-graph-candidate-sources/`](completed/20260805-graph-candidate-sources/STORY.md) |
| C-9 | Matcher subgraph → in-memory Gremlin (`:objs-gremlin-core` / `:objs-gremlin-service`) + Query UI | feature | done | [`completed/20260806-gremlin-subgraph-traversal/`](completed/20260806-gremlin-subgraph-traversal/STORY.md) |
| C-10 | Configurable full-catalog JSON Schema export (options + linked edges) | feature | done | [`completed/20260807-json-schema-generation/`](completed/20260807-json-schema-generation/STORY.md) |
| C-11 | Composer add objects + `obj-expr` / `ids` + visual chain matcher UI | feature | done | [`completed/20260807-composer-draft-shopping/`](completed/20260807-composer-draft-shopping/STORY.md) |
| C-12 | Soft-link subgraphs + snapshot clone (new ids) + Composer save/open/snapshot | feature | done | [`completed/20260807-subgraphs-materialization/`](completed/20260807-subgraphs-materialization/STORY.md) |
| C-13 | Global entity pool + many graphs (no global graph); table renames; graph-local edges | feature | done | [`completed/20260811-graphs-from-objects/`](completed/20260811-graphs-from-objects/STORY.md) |
| C-14 | Schema field flags: drop OBJECT `required` list; `identifier` + `searchable`; identity immutability | feature | done | [`completed/20260811-schema-field-identifiers/`](completed/20260811-schema-field-identifiers/STORY.md) |
| C-15 | Schema migration handover docs (seed reference + JSON Schema → YAML seeds guide) | docs | done | [`completed/20260813-schema-migration-docs/`](completed/20260813-schema-migration-docs/STORY.md) |

---

## ui — Workbench

| # | Item | Type | Status | Source |
|---|------|------|--------|--------|
| U-1 | Unify Schema explorer + linter; object-level edges; top nav | feature | done | [`completed/20260803-schema-workbench-unify/`](completed/20260803-schema-workbench-unify/STORY.md) |
| U-2 | Schemas full-catalog overview + seed import/export | feature | done | [`completed/20260803-schemas-catalog-overview/`](completed/20260803-schemas-catalog-overview/STORY.md) |
| U-3 | Object linter visual workspace + transactional graph mutate | feature | done | [`completed/20260805-object-linter-visual/`](completed/20260805-object-linter-visual/STORY.md) |
| U-4 | Workbench chrome: Explorer Graph vs non-graph + Explore-scope fragment | improvement | done | [`completed/20260811-workbench-chrome-regroup/`](completed/20260811-workbench-chrome-regroup/STORY.md) |
| U-5 | Objects top-level view + shelf → New graph in Composer | feature | done | [`completed/20260813-objects-shelf/`](completed/20260813-objects-shelf/STORY.md) |
| U-6 | Workbench UI Gradle build via node-gradle plugin (`:objs-service-ui`) | improvement | done | [`completed/20260813-ui-gradle-node/`](completed/20260813-ui-gradle-node/STORY.md) |

---

## domain — Concrete graphs / examples

| # | Item | Type | Status | Source |
|---|------|------|--------|--------|
| D-1 | SBOM typed example (toolkit + ontology + REST + SPA) | feature | done | [`completed/20260728-sbom-typed-example/`](completed/20260728-sbom-typed-example/STORY.md), [`docs/design/sbom/example.md`](../design/sbom/example.md) |
| D-2 | SBOM applications inventory app (Apps/Portfolios tabs; portfolio-scoped MI via Gremlin; weak CDX demo) | feature | in-progress | [`in-progress/sbom-inventory-app/`](in-progress/sbom-inventory-app/STORY.md) |
| D-3 | Asset repository example (collections-as-graphs object store; domain REST + simple explore UI) | feature | done | [`completed/20260814-asset-repository-example/`](completed/20260814-asset-repository-example/STORY.md), [`docs/design/asset-repository/example.md`](../design/asset-repository/example.md) |
| D-4 | Asset repository demo seeds, qsynth load kit, collection query exec stats | feature | done | [`completed/20260814-asset-repository-demo-seeds/`](completed/20260814-asset-repository-demo-seeds/STORY.md) |
| D-5 | Asset repository Postgres search, SPA refresh routing, batched load.py | fix | done | [`completed/20260814-asset-repository-ops-fixes/`](completed/20260814-asset-repository-ops-fixes/STORY.md) |

---

## platform — Build / tooling

| # | Item | Type | Status | Source |
|---|------|------|--------|--------|
| P-1 | Gradle cleanup: `platform()` BOM, prune deps, minimize plugins | improvement | done | [`completed/20260813-build-system-cleanup/`](completed/20260813-build-system-cleanup/STORY.md) |

---

## Summary

| Status | Count |
|--------|------:|
| backlog | 0 |
| planned | 0 |
| in-progress | 1 |
| done | 23 |
