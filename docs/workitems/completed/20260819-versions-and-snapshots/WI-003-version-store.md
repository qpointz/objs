# WI-003 — Version store

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Core  
**Status:** done  
**Depends on:** WI-002  
**Examples:** **SBOM + AR** (must stay green; no fingerprint rewire yet)

## Goal

Flyway **V4**: version tables exist, but **persist stays today’s in-place HEAD** (no auto version row). `head_version` nullable. Version key `(parent_id, version BIGINT)` when a capture happens. **No** deep freeze yet. **`clone()` stays** (deep copy of HEAD; new ids; no version rows).

## Schema ([`ER.md`](ER.md))

- HEAD: keep content; add nullable `head_version` + composite FK when non-null.
- `*_version` tables created; empty until WI-004 capture (except optional unused).
- Do **not** backfill version rows. Greenfield only; no history until first `createDeepGraphVersion`.

## Persist

Create/update/delete HEAD as today (plus clocks from WI-002). Call `BomVersioningStrategy.shouldCapture`; C-18 default is **false**. Do **not** insert `*_version` or change `head_version` unless the strategy says so.

Ship interface + `ExplicitOnlyVersioningStrategy`. No `OnWrite` / per-graph impl in this WI.

## Tests

- Create + update entity: still one `bom_entity` row; `bom_entity_version` count 0; `head_version` null
- `copyGraph` / `mergeGraph` / `clone()` unchanged (clone still new ids; `head_version` null on copies)
- Flyway V4 applies; HEAD FKs allow null `head_version`
- `:sbom-service:test` `:asset-repository-service:test` still pass (in-place persist)
