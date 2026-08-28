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

SQL column `type` is drawn as `obj_type` (`type` breaks Mermaid ER).

```mermaid
erDiagram
  direction LR

  objs_entity_schema ||--o{ objs_entity : "HEAD schema"
  objs_entity_schema ||--o{ objs_entity_version : "history schema"
  objs_entity_schema ||--o{ objs_edge_schema : "optional props schema"
  objs_edge_schema }o--o{ objs_graph_edge : "allow-list HEAD"
  objs_edge_schema }o--o{ objs_graph_edge_version : "allow-list history"

  objs_entity ||--o{ objs_entity_version : "head_and_history"
  objs_entity ||--o{ objs_graph_entity : "live member"
  objs_entity ||--o{ objs_graph_edge : "live endpoint"
  objs_entity_version ||--o{ objs_graph_version_member : "pinned entity version"

  objs_graph ||--o{ objs_graph_version : "head_and_history"
  objs_graph ||--o{ objs_graph_entity : "members"
  objs_graph ||--o{ objs_graph_edge : "owns"

  objs_graph_version ||--o{ objs_graph_version_member : "deep members"
  objs_graph_version ||--o{ objs_graph_version_edge : "deep edges"

  objs_graph_edge ||--o{ objs_graph_edge_version : "head_and_history"
  objs_graph_edge_version ||--o{ objs_graph_version_edge : "pinned edge version"

  objs_entity {
    uuid id PK
    bigint head_version FK
    string obj_type
    string schema_version
    string payload
    string annotations
    datetime created_at
    datetime updated_at
  }

  objs_entity_version {
    uuid entity_id PK
    bigint version PK
    string obj_type
    string schema_version
    string payload
    string annotations
    datetime created_at
    datetime updated_at
    datetime head_deleted_at
  }

  objs_graph {
    uuid id PK
    bigint head_version FK
    string annotations
    datetime created_at
    datetime updated_at
  }

  objs_graph_version {
    uuid graph_id PK
    bigint version PK
    string graph_annotations
    string annotations
    datetime created_at
    datetime updated_at
    datetime head_deleted_at
  }

  objs_graph_version_member {
    uuid graph_id PK
    bigint graph_version PK
    uuid entity_id PK
    bigint entity_version FK
    datetime created_at
    datetime updated_at
  }

  objs_graph_version_edge {
    uuid graph_id PK
    bigint graph_version PK
    uuid edge_id PK
    bigint edge_version FK
    datetime created_at
    datetime updated_at
  }

  objs_graph_entity {
    uuid graph_id PK
    uuid entity_id PK
    datetime created_at
    datetime updated_at
  }

  objs_graph_edge {
    uuid id PK
    uuid graph_id FK
    bigint head_version FK
    uuid source_id
    uuid target_id
    string role
    string obj_type
    string schema_version
    string properties
    datetime created_at
    datetime updated_at
  }

  objs_graph_edge_version {
    uuid edge_id PK
    bigint version PK
    uuid graph_id
    uuid source_id
    uuid target_id
    string role
    string obj_type
    string schema_version
    string properties
    datetime created_at
    datetime updated_at
    datetime head_deleted_at
  }
```

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

## Clocks

Every `objs_*` table has `created_at` / `updated_at` `TIMESTAMP NOT NULL`. Persist owns values; client JSON is ignored. Version-row `updated_at` equals `created_at` (append-only). Graph HEAD `updated_at` also bumps on live membership/edge change.

## Indexes

- `objs_entity (type, schema_version)`
- `objs_graph_entity (entity_id)`
- `objs_graph_edge (graph_id)`, `(source_id)`, `(target_id)`, `(role)`, `(graph_id, source_id)`, `(graph_id, target_id)`
- PostgreSQL GIN `jsonb_path_ops` on `objs_entity.annotations` and `objs_graph.annotations`
- `objs_seed_ledger (last_attempt_status)`

## Catalog / seed (unchanged shape)

`objs_entity_schema` PK `(type, version)`; V2 adds `tags`, `attributes`.
`objs_edge_schema` PK `(source_type, role, target_type)`; V2 adds description, verbs, tags, attributes.
`objs_seed_ledger` PK `seed_key`.

## Versioning SPI

`BomVersioningStrategy.shouldCapture(ctx)`. C-18 bean: `ExplicitOnlyVersioningStrategy` — always false on ordinary persist. `createDeepGraphVersion` always captures regardless.

## Provenance

Deep pins keep the **original** `entity_id` / `edge_id` plus a version. Freeze does not allocate new pool identities. `clone()` does allocate new ids and starts with `head_version` NULL.

## App tables

SBOM/AR Flyway is a separate history (`flyway_schema_history`). SBOM fingerprints store `(graph_id, graph_version)` on `sbom_application_fingerprint` (app Flyway V2).
