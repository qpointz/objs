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

---

## ui — Workbench

| # | Item | Type | Status | Source |
|---|------|------|--------|--------|
| U-1 | Unify Schema explorer + linter; object-level edges; top nav | feature | done | [`completed/20260803-schema-workbench-unify/`](completed/20260803-schema-workbench-unify/STORY.md) |
| U-2 | Schemas full-catalog overview + seed import/export | feature | done | [`completed/20260803-schemas-catalog-overview/`](completed/20260803-schemas-catalog-overview/STORY.md) |
| U-3 | Object linter visual workspace + transactional graph mutate | feature | done | [`completed/20260805-object-linter-visual/`](completed/20260805-object-linter-visual/STORY.md) |

---

## domain — Concrete graphs / examples

| # | Item | Type | Status | Source |
|---|------|------|--------|--------|
| D-1 | SBOM typed example (toolkit + ontology + REST + SPA) | feature | done | [`completed/20260728-sbom-typed-example/`](completed/20260728-sbom-typed-example/STORY.md), [`docs/design/sbom/example.md`](../design/sbom/example.md) |

---

## Summary

| Status | Count |
|--------|------:|
| backlog | 0 |
| planned | 0 |
| in-progress | 0 |
| done | 12 |
