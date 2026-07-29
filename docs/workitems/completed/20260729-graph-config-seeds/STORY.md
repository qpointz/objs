# Story: PostgreSQL persistence and graph configuration seeds

**Slug:** `graph-config-seeds`  
**Branch:** `graph-config-seeds`  
**Status:** completed
**Backlog:** C-3, C-4  
**Design:** [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md), [`docs/design/graph/model.md`](../../../design/graph/model.md), [`docs/design/graph/object-schema-dsl.md`](../../../design/graph/object-schema-dsl.md), [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md)
**Gaps:** [`GAPS.md`](GAPS.md)

## Goal

Deliver durable graph storage and declarative initial graph configuration in two strictly ordered
stages:

1. Verify and complete PostgreSQL persistence for entities and edges, then persist the schema and
   allowed-edge registries.
2. Add qpointz-inspired, extensible multi-document seeds for schemas, allowed-edge rules, graphs,
   and future document kinds.

Stage 2 must not start until Stage 1 is complete, pushed, manually tested by the user against
PostgreSQL, and explicitly approved.

## Stages

| Stage | Work items | Readiness | Exit condition |
|-------|------------|-----------|----------------|
| 1 — PostgreSQL persistence | WI-001–WI-002 | Ready after remaining Stage 1 gap clarifications | Automated checks pass; user completes manual PostgreSQL test checklist and approves Stage 2 |
| 2 — Seed implementation | WI-003–WI-006 | Stage 1 accepted; Stage 2 defaults accepted | Durable seed pipeline and SBOM example integration pass |

## Work Items

### Stage 1 — PostgreSQL persistence

- [x] WI-001 — PostgreSQL baseline and registry storage (`WI-001-postgres-registry-storage.md`)
- [x] WI-002 — Durable catalogs and Stage 1 verification (`WI-002-durable-catalogs-verification.md`)

### Pre-Stage 2 UI workbench

- [x] Schema explorer + visual/source linter in `objs-sbom-example/ui`
  - separate Graph explorer, Schema explorer, and Schema linter views
  - entity/edge-property usages, directional allowed edges, DSL JSON/YAML, JSON Schema projection
  - version update vs next-major create; graph inspector links into schema detail

### Stage 2 — Seed implementation

- [x] WI-003 — Multi-document seed format and importer (`WI-003-seed-format-importer.md`)
- [x] WI-004 — Seed ledger and startup loading (`WI-004-seed-ledger-startup.md`)
- [x] WI-005 — Seed import/export REST API (`WI-005-seed-rest-api.md`)
- [x] WI-006 — SBOM seed migration and design documentation (`WI-006-sbom-seeds-docs.md`)

## Scope

- PostgreSQL/Flyway persistence for entity, edge, schema, allowed-edge-rule, and seed-ledger data
- Consumer-facing registry/catalog abstractions; production uses PostgreSQL-authoritative
  implementations with in-memory write-through caches
- Authoritative qpointz-inspired object-schema DSL with deterministic JSON Schema projection
- Versioned, kind-discriminated multi-document YAML
- Ordered `classpath:` and `file:` startup resources
- Fingerprint-based, idempotent startup application
- `MERGE` import semantics
- REST import/export
- Canonical SBOM ontology and demo graph migrated to the shared seed mechanism

## Out of scope

- Authentication and authorization
- Cloud seed resources such as S3, GCS, and Azure Blob
- `REPLACE` import semantics
- Metadata facets or scopes from qpointz
- Semantic seed diffs
- General graph synchronization or pruning

