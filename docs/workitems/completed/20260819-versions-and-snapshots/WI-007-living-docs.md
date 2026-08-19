# WI-007 — Living docs

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 7 — Docs  
**Status:** done  
**Depends on:** WI-002…WI-006  
**Examples:** **docs**

## Goal

Sweep living design after the code is in. Confirm: flags are not the design; freeze is deep graph version; clocks are C-18; DIY versions at your own risk. Do **not** archive the story or mark BACKLOG `done` until asked.

**Examples:** `./gradlew :sbom-service:test :asset-repository-service:test` still pass before this WI is `[x]`.

## Detailed database model (required)

Create **[`docs/design/graph/database-model.md`](../../../design/graph/database-model.md)** as the as-built schema doc (not a sketch). Source of truth after ship: Flyway SQL + JPA, not the story [`ER.md`](ER.md) (keep ER as historical lock).

Must include:

- Mermaid ER (`direction LR`; SQL column `type` may be aliased in the diagram only)
- Every `bom_*` table: columns, types, nullability, PKs, FKs, indexes (GIN)
- HEAD vs `*_version` vs deep-freeze children (`bom_graph_version_member` / `_edge`)
- `head_version` nullable; `(parent_id, version)` uniqueness; `version` not globally unique
- Versioning SPI vs persist (default no auto-version; capture copies HEAD)
- Provenance: pins keep original entity/edge ids
- Clocks on all `bom_*`
- Flyway V3/V4, **greenfield only** (no data migration)
- Link from [`persistence.md`](../../../design/graph/persistence.md) and [`docs/design/graph/README.md`](../../../design/graph/README.md)

## Also sweep

- [`model.md`](../../../design/graph/model.md), [`apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md)
- [`docs/design/sbom/example.md`](../../../design/sbom/example.md)
- REST + UI design notes
- Public docs under `docs/public/` if freeze/fingerprint is user-facing
