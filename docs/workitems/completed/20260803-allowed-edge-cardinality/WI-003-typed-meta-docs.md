# WI-003 — Typed meta, example, and graph design docs

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Typed meta + docs  
**Status:** done  
**Depends on:** WI-000, WI-001, WI-002

## Goal

Align the typed toolkit and durable design docs with allowed-edge cardinality; keep the SBOM
example backward-compatible (default `UNSPECIFIED`) unless illustrative values are set deliberately.

## Scope

- Add `cardinality` to `TypedEdgeMeta` (default `UNSPECIFIED`)
- SBOM `edgeRule` / seeds: omit (default) or set a few illustrative `1:*` / `1:1` values if useful
- Update design docs:
  - [`docs/design/graph/model.md`](../../../design/graph/model.md)
  - [`docs/design/graph/validation.md`](../../../design/graph/validation.md)
  - [`docs/design/graph/seeds.md`](../../../design/graph/seeds.md)
  - [`docs/design/service/rest-api.md`](../../../design/service/rest-api.md) if registry shapes need a note
- Ensure story decisions are reflected (metadata only; no persist count checks)

## Out of scope

- Persist-time cardinality enforcement
- Instance graph explorer enrichment from allow-list rules

## Acceptance

- [x] `TypedEdgeMeta` carries cardinality with default `UNSPECIFIED`
- [x] Existing SBOM behaviour remains valid when cardinality is omitted
- [x] Graph / seeds / REST design docs describe the three values and semantics
