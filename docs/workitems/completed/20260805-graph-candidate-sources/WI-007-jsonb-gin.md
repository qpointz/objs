# WI-007 — JSONB + GIN + drop H2

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 7 — Postgres  
**Status:** done  
**Depends on:** WI-003

## Goal

Migrate graph JSON columns to **JSONB**, add **GIN** on annotations for containment sources, and drop H2 as a supported graph-query/runtime assumption.

## Scope

- Flyway Java migration `V4__bom_graph_jsonb_gin` (Postgres only; no-op on H2): `payload` / `annotations` / edge `properties` → `JSONB`.
- GIN index on `bom_graph_entity.annotations` using **`jsonb_path_ops`** (containment `@>`).
- Graph SQL pushdown assumes PostgreSQL; H2 unit tests remain local-eval smoke only.
- Complementary to WI-006 (not a substitute).

## Out of scope

- Payload GIN / payload SQL matchers
- Dual H2+Postgres graph-query dialects
- API pagination / result-size caps / sparse HTTP projection (compensating follow-up; see STORY)

## Acceptance

- [x] Migrations apply cleanly on Postgres (Testcontainers IT)
- [x] Anno source pushdown uses JSONB; GIN index present (`idx_bom_graph_entity_annotations_gin`)
- [x] H2 is not assumed for graph SQL pushdown (containment source returns null → local eval)
