# Database model (as-built)

**Status:** C-18 shipped schema  
**Source of truth:** Flyway SQL + JPA in `objs-core`, not the story [`ER.md`](../../workitems/completed/20260819-versions-and-snapshots/ER.md) (that file is the historical lock).  
**Vendors:** PostgreSQL (JSONB + GIN) and H2 (JSON, no GIN). Same tables.  
V6 migrates existing databases from the former `bom_*` table names to the `objs_*` namespace.

Objs Flyway: `classpath:org/poc/objs/core/db/migration/{vendor}` into `flyway_schema_history_objs`.

| Version | File | What |
|---------|------|------|
| V1 | `V1__bom_schema.sql` | Pool, graphs, membership, edges, catalog, seed ledger |
| V2 | `V2__catalog_metadata.sql` | Catalog envelope tags/attributes/verbs |
| V3 | `V3__audit_clocks.sql` | `created_at` / `updated_at` on HEAD tables that lacked them |
| V4 | `V4__version_store.sql` | `*_version` tables, nullable `head_version`, deep-freeze pins |
| V5 | `V5__graph_version_member_entity_index.sql` | Reverse lookup index for deep-freeze pins |
| V6 | `V6__rename_bom_tables_to_objs.sql` | Rename all Objs persistence tables to the `objs_*` namespace |

Live GET never joins `*_version`. Default persist is in-place HEAD (`ExplicitOnlyVersioningStrategy`). Capture is `createDeepGraphVersion` (Composer **Create version**). `clone()` copies HEAD into new ids and does not copy `*_version`.

## Diagram

SQL column `type` on instance tables is drawn as `obj_type` (`type` breaks Mermaid ER syntax).
JSON columns are `jsonb` on PostgreSQL and `json` on H2.

**Physical FKs only** in the diagram. Catalog tables (`objs_entity_schema`, `objs_edge_schema`) and
`objs_seed_ledger` have **no** foreign keys to pool/graph instance rows; allow-list and validation are
application-level.

```mermaid
erDiagram
  direction LR

  objs_entity ||--o{ objs_entity_version : "head_and_history"
  objs_entity ||--o{ objs_graph_entity : "live_member"
  objs_entity ||--o{ objs_graph_edge : "live_endpoint"

  objs_graph ||--o{ objs_graph_version : "head_and_history"
  objs_graph ||--o{ objs_graph_entity : "members"
  objs_graph ||--o{ objs_graph_edge : "owns"

  objs_graph_version ||--o{ objs_graph_version_member : "deep_members"
  objs_graph_version ||--o{ objs_graph_version_edge : "deep_edges"

  objs_graph_edge ||--o{ objs_graph_edge_version : "head_and_history"
  objs_graph_edge_version ||--o{ objs_graph_version_edge : "pinned_edge_version"
  objs_entity_version ||--o{ objs_graph_version_member : "pinned_entity_version"

  objs_entity {
    uuid id PK
    bigint head_version FK
    varchar obj_type
    varchar schema_version
    jsonb payload
    jsonb annotations
    timestamp created_at
    timestamp updated_at
  }

  objs_entity_version {
    uuid entity_id PK
    bigint version PK
    varchar obj_type
    varchar schema_version
    jsonb payload
    jsonb annotations
    timestamp created_at
    timestamp updated_at
    timestamp head_deleted_at
  }

  objs_graph {
    uuid id PK
    bigint head_version FK
    jsonb annotations
    timestamp created_at
    timestamp updated_at
  }

  objs_graph_version {
    uuid graph_id PK
    bigint version PK
    jsonb graph_annotations
    jsonb annotations
    timestamp created_at
    timestamp updated_at
    timestamp head_deleted_at
  }

  objs_graph_entity {
    uuid graph_id PK
    uuid entity_id PK
    timestamp created_at
    timestamp updated_at
  }

  objs_graph_edge {
    uuid id PK
    uuid graph_id FK
    bigint head_version FK
    uuid source_id
    uuid target_id
    varchar role
    varchar obj_type
    varchar schema_version
    jsonb properties
    timestamp created_at
    timestamp updated_at
  }

  objs_graph_edge_version {
    uuid edge_id PK
    bigint version PK
    uuid graph_id
    uuid source_id
    uuid target_id
    varchar role
    varchar obj_type
    varchar schema_version
    jsonb properties
    timestamp created_at
    timestamp updated_at
    timestamp head_deleted_at
  }

  objs_graph_version_member {
    uuid graph_id PK
    bigint graph_version PK
    uuid entity_id PK
    bigint entity_version FK
    timestamp created_at
    timestamp updated_at
  }

  objs_graph_version_edge {
    uuid graph_id PK
    bigint graph_version PK
    uuid edge_id PK
    bigint edge_version FK
    timestamp created_at
    timestamp updated_at
  }

  objs_entity_schema {
    varchar obj_type PK
    varchar version PK
    jsonb definition_doc
    varchar usage
    jsonb tags
    jsonb attributes
    timestamp created_at
    timestamp updated_at
  }

  objs_edge_schema {
    varchar source_type PK
    varchar role PK
    varchar target_type PK
    varchar properties_policy
    boolean empty_properties_allowed
    varchar properties_schema_type
    varchar properties_schema_version
    varchar cardinality
    text description
    varchar source_verb
    varchar target_verb
    jsonb tags
    jsonb attributes
    timestamp created_at
    timestamp updated_at
  }

  objs_seed_ledger {
    varchar seed_key PK
    varchar last_success_fingerprint
    timestamp last_success_at
    varchar last_attempt_fingerprint
    varchar last_attempt_status
    timestamp last_attempt_at
    text last_error
    timestamp created_at
    timestamp updated_at
  }
```

### Logical catalog links (not persisted as FKs)

| Catalog row | Governs (runtime) |
|-------------|-------------------|
| `objs_entity_schema (type, version)` | `objs_entity` / `objs_entity_version` `obj_type` + `schema_version` |
| `objs_edge_schema (source_type, role, target_type)` | Allowed live/history edges; optional `properties_schema_*` for `objs_graph_edge.properties` |
| `objs_seed_ledger.seed_key` | Idempotent seed import bookkeeping only |

## HEAD vs history vs pins

| Kind | Tables | Read path |
|------|--------|-----------|
| Live HEAD | `objs_entity`, `objs_graph`, `objs_graph_entity`, `objs_graph_edge` | Graph GET / pool GET |
| History | `objs_entity_version`, `objs_graph_version`, `objs_graph_edge_version` | Capture + reconstruct |
| Deep freeze children | `objs_graph_version_member`, `objs_graph_version_edge` | Pins original entity/edge **ids** + `version` |

`head_version` is nullable. NULL means never captured. When set, composite FK `(id, head_version) → *_version(parent_id, version)` (MATCH SIMPLE: NULL skips the check).

Version PK is `(parent_id, version BIGINT)`. `version` is **not** globally unique. `nextVersion(prev) = max(epochMillis, (prev ?: 0) + 1)`.

Version parent ids have **no** FK back to HEAD. After DELETE HEAD, reconstruct still works.

V4 drops `objs_graph_edge` FKs to `objs_entity` (source/target) so deleting HEAD entities does not `RESTRICT` on live edges; pins remain on `*_version`.

Nullable columns (no `NOT NULL` in DDL): `objs_entity.head_version`, `objs_graph.head_version`,
`objs_graph_edge.head_version`, `objs_graph_edge.type`, `objs_graph_edge.schema_version`,
`objs_graph_edge.properties`, matching columns on `*_version` history rows, `head_deleted_at` on
history tables, and most `objs_seed_ledger` attempt/success fields except `last_attempt_status`,
`last_attempt_at`, and clocks.

## Clocks

Every `objs_*` table has `created_at` / `updated_at` `TIMESTAMP NOT NULL`. Persist owns values; client JSON is ignored. Version-row `updated_at` equals `created_at` (append-only). Graph HEAD `updated_at` also bumps on live membership/edge change.

## Indexes

- `objs_entity (type, schema_version)` — `idx_objs_entity_type_schema_version`
- `objs_graph_entity (entity_id)` — `idx_objs_graph_entity_entity`
- `objs_graph_edge (graph_id)`, `(source_id)`, `(target_id)`, `(role)`, `(graph_id, source_id)`, `(graph_id, target_id)`
- `objs_graph_version_member (entity_id)` — `idx_objs_graph_version_member_entity_id` (V5)
- PostgreSQL GIN `jsonb_path_ops` on `objs_entity.annotations` and `objs_graph.annotations`
- `objs_seed_ledger (last_attempt_status)` — `idx_objs_seed_ledger_status`

## Catalog / seed

| Table | PK | Notable columns |
|-------|-----|-----------------|
| `objs_entity_schema` | `(type, version)` | `definition_doc`, `usage`, `tags`, `attributes` (V2), clocks |
| `objs_edge_schema` | `(source_type, role, target_type)` | `properties_policy`, `empty_properties_allowed`, `properties_schema_type/version`, `cardinality`, `description`, `source_verb`, `target_verb`, `tags`, `attributes` (V2), clocks |
| `objs_seed_ledger` | `seed_key` | success/attempt fingerprints and timestamps, `last_attempt_status`, `last_error`, clocks |

JPA: `SchemaCatalogRecord`, `AllowedEdgeRuleRecord`, `SeedLedgerRecord` in `objs-core` persistence package.

## Versioning SPI

`BomVersioningStrategy.shouldCapture(ctx)`. C-18 bean: `ExplicitOnlyVersioningStrategy` — always false on ordinary persist. `createDeepGraphVersion` always captures regardless.

## Provenance

Deep pins keep the **original** `entity_id` / `edge_id` plus a version. Freeze does not allocate new pool identities. `clone()` does allocate new ids and starts with `head_version` NULL.

## App tables

SBOM/AR Flyway is a separate history (`flyway_schema_history`). SBOM fingerprints store `(graph_id, graph_version)` on `sbom_application_fingerprint` (app Flyway V2).
