# WI-003 — Anno as source-capable pushdown

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Anno source  
**Status:** done  
**Depends on:** WI-002

## Goal

Make `anno` / `MatchAllAnnotationMatcher` **source-capable**: compile annotation equality/And into a Postgres containment query that minimizes the initial candidate set.

## Scope

- `MatchAllAnnotationMatcher` implements `BoMSourceCapableMatcher`.
- Pushdown via `backend.annotationContainmentSource(filter)` (JSONB `@>`), not a reader `is BoMPushableMatcher` branch.
- H2 / non-Postgres: `toCandidateSource` returns null → all-entities fallback + in-memory `matches`.
- DSL key `anno` unchanged.

## Out of scope

- `anno-expr` SQL pushdown
- GIN index creation (WI-007)
- JSONB column migration (WI-007)
- API pagination / result-size caps / sparse HTTP projection

## Acceptance

- [x] First-stage `anno` on Postgres uses SQL containment source
- [x] Same subgraph results for `anno` filters (store tests green)
- [x] Focused tests for source-capable vs non-Postgres null source
