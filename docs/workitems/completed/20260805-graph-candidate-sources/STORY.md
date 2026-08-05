# Story: Graph candidate sources and load performance

**Slug:** `graph-candidate-sources`  
**Branch:** `graph-candidate-sources`  
**Status:** completed  
**Backlog:** C-8  
**Design:** [`docs/design/graph/annotations-and-subgraphs.md`](../../../design/graph/annotations-and-subgraphs.md), [`docs/design/graph/persistence.md`](../../../design/graph/persistence.md), [`docs/design/ui.md`](../../../design/ui.md)  
**Depends on:** [`matcher-query-language`](../20260729-matcher-query-language/STORY.md) (merged)

## Goal

Focus this story on **backend and code performance** for graph selection: rewrite the **execution plan** so selection is **candidate source → filters → induced edges**, not a pushable/non-pushable type flag on matchers. Improve load cost with bounded edge SQL, lazy/deferred candidate JSON, and Postgres JSONB + GIN. Matcher **DSL stays frozen**.

Stage **1** is a **separate prerequisite** (not performance): host the workbench UI in `:objs-service` and fix `npm run build`. **Do not start Stage 2+ until Stage 1 is complete and the manual-test readiness gate below is satisfied.**

## Confirmed decisions

| Topic | Choice |
|-------|--------|
| Focus | **Backend / execution / code path** — make selection cheaper; not API envelope redesign |
| Stage order | **Stage 1 (UI prerequisite) completes first** → readiness gate → then Stage 2+ performance WIs |
| Prerequisite | Move workbench SPA + serving into `:objs-service`; fix `npm run build` (blocks Gradle UI sync / restarts) |
| Scope | **Execution plan only** — `anno` / `anno-expr` / ordered arrays and `POST /graph/query` bodies unchanged |
| API response shape | **Out of this story** — pagination, result-size caps, and sparse projection are **compensating** controls if backend gains are still insufficient; track as a later backlog item, not a substitute for this work |
| Taxonomy | Replace pushable/non-pushable with **candidate source** + **filter**; first source-capable stage owns pushdown |
| Fallback | If stage 0 is not source-capable → `AllEntities` source, then `matches` in order |
| Later stages | Filter-only in v1 (no re-source / multi-stage pushdown) |
| Compatibility | **Hard break** on obsolete internal Kotlin types — delete, no `@Deprecated`, no shims; migrate in-repo callers |
| Edges (query) | Bound JDBC induced-edge load by selected ids — **not** full table scan; **not** JPA `@EntityGraph` |
| Edges (delete) | Replace per-entity `findAll` cascade with one incident-edge query / bulk delete |
| Lazy JSON | Raw-backed candidates; no Jackson for excluded rows; defer unused columns until survivors |
| Storage | JSON → **JSONB** + **GIN** on annotations; drop H2 as graph-query/runtime assumption |
| Cleanup | Mandatory inspect + delete dead matcher types before story closure |

## Stages

| Stage | Work items | Exit condition |
|-------|------------|----------------|
| 0 — Scaffold | WI-000 | Story + backlog + milestone |
| **1 — Manual-test readiness (prerequisite)** | **WI-001** | Workbench in `:objs-service`; `npm run build` green; UI without SBOM example; **readiness gate passed** |
| 2 — Source model | WI-002 | `BoMCandidateSource` + AllEntities fallback wired in reader |
| 3 — Anno source | WI-003, WI-010 | Anno / match-all and lowerable `anno-expr` as source-capable (SQL containment) |
| 4 — Chains | WI-004 | Chain + `anno-expr` filters; DSL parity tests |
| 5 — Edges | WI-005 | Bound induced + incident edge loading (JDBC) |
| 6 — Lazy JSON | WI-006 | Exclude without deserialize; deferred columns |
| 7 — Postgres | WI-007 | JSONB + GIN; drop H2 for graph query |
| 8 — Cleanup | WI-008 | Dead matcher types removed |
| 9 — Docs | WI-009 | Design docs + benchmarks |

### Stage 1 → Stage 2 gate (ready for testing)

After WI-001 is `[x]`, confirm all of the following **before** starting WI-002 (or any later performance WI):

1. `npm run build` (under `:objs-service` UI) succeeds; Gradle UI sync does not fail processResources/restart.
2. `./gradlew :objs-app:run` (or equivalent) serves **`http://localhost:8080/workbench/`**.
3. Workbench loads Explorer / Composer / Schema routes; legacy `/ui/**` redirects still work.
4. Graph query from Explorer/Composer hits foundation `POST /api/v1/objs/graph/query` successfully (smoke).
5. You can iterate UI + backend restart without depending on `:objs-sbom-example` for the SPA/static handlers.

Until this gate is green, treat Stage 2+ as **blocked**.

## Work Items

- [x] WI-000 — Story scaffolding (`WI-000-story-scaffold.md`)
- [x] WI-001 — Workbench UI in objs-service + fix npm build (`WI-001-workbench-ui-service.md`) — **Stage 1 complete; readiness gate confirmed**
- [x] WI-002 — Candidate source + AllEntities fallback (`WI-002-candidate-source.md`)
- [x] WI-003 — Anno as source-capable pushdown (`WI-003-anno-source.md`)
- [x] WI-010 — Anno-expr as source-capable pushdown (`WI-010-anno-expr-source.md`)
- [x] WI-004 — Chained filters + DSL parity (`WI-004-chain-filters.md`)
- [x] WI-005 — Bound edge loading (`WI-005-bound-edges.md`)
- [x] WI-006 — Lazy / deferred candidate JSON (`WI-006-lazy-candidate-json.md`)
- [x] WI-007 — JSONB + GIN + drop H2 (`WI-007-jsonb-gin.md`)
- [x] WI-008 — Matcher package cleanup (`WI-008-matcher-cleanup.md`)
- [x] WI-009 — Design docs and benchmarks (`WI-009-docs-benchmarks.md`)

## Scope

- **Stage 1 prerequisite:** move workbench UI + SPA static handlers into `:objs-service`; fix UI TypeScript/`npm run build`; pass readiness gate
- **Stage 2+ backend / code-path performance** (see Confirmed decisions)
- Core match/persistence execution model (`BoMCandidateSource`, reader/store wiring)
- Bound induced-edge and incident-edge SQL
- Lazy/deferred candidate JSON materialization
- Postgres JSONB migration + GIN; drop H2 for graph-query fidelity
- Hard delete of obsolete matcher taxonomy types
- Design doc updates (`annotations-and-subgraphs.md`, `persistence.md`, `ui.md` / example notes for UI location)

## Out of scope

- Any matcher DSL key/shape changes
- **API response-shape work** (pagination, result-size limits, sparse/projected HTTP payloads) — compensating follow-up if backend improvement is not enough; not part of C-8
- Multi-stage re-pushdown
- Payload-field SQL pushdown as a general DSL feature
- UI large-graph UX / new workbench features beyond the module move + build fix
- JPA entity↔edge associations + `@EntityGraph` for `/graph/query`
- SBOM `listApplications` / other example `loadAll` hotspots (separate cleanup unless they block core work)
- Removing `:objs-sbom-example` from `:objs-app` (optional later; UI must not require the example module)
