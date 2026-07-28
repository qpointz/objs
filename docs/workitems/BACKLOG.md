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
| C-3 | Persist central schema catalog (type+version) + allowed-edge rules as PostgreSQL tables | feature | backlog | [`docs/design/graph/model.md`](../design/graph/model.md) (G-6/G-8) |

---

## domain — Concrete graphs / examples

| # | Item | Type | Status | Source |
|---|------|------|--------|--------|
| D-1 | SBOM typed example (toolkit + ontology + REST + SPA) | feature | done | [`completed/20260728-sbom-typed-example/`](completed/20260728-sbom-typed-example/STORY.md), [`docs/design/sbom/example.md`](../design/sbom/example.md) |

---

## Summary

| Status | Count |
|--------|------:|
| backlog | 1 |
| planned | 0 |
| in-progress | 0 |
| done | 3 |
