# WI-002 — Audit clocks

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Audit  
**Status:** done  
**Depends on:** WI-001  
**Examples:** **workbench + SBOM + AR** (JSON fields)

## Goal

Store-owned `created_at` / `updated_at` on every `bom_*` table that lacks them **before** the version split. Flyway **V3**, both vendors. Prerequisite for versioning (append time has a column).

## Schema

`ALTER` `bom_entity`, `bom_graph`, `bom_graph_entity`, `bom_graph_edge`: `created_at` / `updated_at` `NOT NULL DEFAULT CURRENT_TIMESTAMP`. Schema only — **no data backfill**. Greenfield: recreate the DB. Catalog/seed already have clocks — do not duplicate; verify JPA still stamps them.

## Persist / domain / REST

- Persist owns values; client JSON ignored.
- Insert: both = now. Update of **that** row: `created_at` unchanged, `updated_at` = now.
- Graph `updated_at` also bumps on live membership/edge change. Entity `updated_at` does **not** bump merely because a graph linked it.
- `copyGraph` / `mergeGraph`: new graph/edges get now; **shared pool entities keep clocks**.
- Expose `createdAt` / `updatedAt` on entity, edge, graph JSON. SBOM/AR DTOs read **store** clocks (not ontology payload clocks).

## Tests

Insert sets both; second write keeps `createdAt`, advances `updatedAt`; client stamps ignored; copyGraph preserves entity clocks; H2 + Postgres IT for defaults.

**Not this WI:** version tables, `head_version`, deep freeze. Payload still in-place.

**Examples:** `:sbom-service:test` and `:asset-repository-service:test` still pass (clocks on store JSON must not break DTOs).
