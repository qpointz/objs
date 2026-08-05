# WI-006 — Lazy / deferred candidate JSON

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 6 — Lazy JSON  
**Status:** done  
**Depends on:** WI-002

## Goal

Ensure excluded candidates do not pay Jackson (or unused column) cost on the **backend selection path**. Strengthen raw-backed candidates and column projection driven by the execution plan. Do not change the public HTTP subgraph envelope.

## Scope

- Keep/extend raw-backed entity/edge candidates (string / `PGobject` / bytes); map views parse lazily.
- Prefer filter checks that avoid full-map parse when only key equality / containment is needed.
- **Column projection:** if no stage needs `payload`, do not select it until survivors (second fetch or defer until `toDomain`).
- When annotations were fully applied by SQL source and later stages do not need them, defer annotation materialization similarly (reload for response if required).
- Tests: `parseInvocations == 0` (or no payload column read) for candidates excluded by early filters.

## Out of scope

- JSONB/GIN migration (WI-007)
- Changing HTTP response shape for survivors (still full domain maps when returned)
- API pagination / result-size caps / sparse projection as a public API feature (compensating follow-up; see STORY)

## Acceptance

- [x] Excluded candidates do not Jackson-parse payload/annotations in covered scenarios
- [x] Survivors still round-trip correctly on `POST /graph/query` (store selectSubgraph)
- [x] Unit tests assert lazy/defer behavior
