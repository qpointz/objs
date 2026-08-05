# WI-005 — Bound edge loading

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Edges  
**Status:** done  
**Depends on:** WI-002

## Goal

Stop full-table edge scans for induced subgraph reads and for entity-delete cascade. Keep the JDBC read path; do **not** introduce JPA `@EntityGraph`.

## Scope

- Induced edges: JDBC `WHERE source_id IN (…) AND target_id IN (…)` with chunking (`IN_CHUNK_SIZE`).
- Delete cascade: `findBySourceIdInOrTargetIdIn` once per delete batch instead of `findAll` per entity.
- Still apply `matchesEdge` after SQL for custom edge policies.

## Out of scope

- JPA associations / `@EntityGraph`
- Changing induced-subgraph semantics
- API pagination / result caps

## Acceptance

- [x] Query path uses id-bounded induced-edge SQL
- [x] Delete cascade uses incident-edge repository query
- [x] Store mutate/delete tests green
