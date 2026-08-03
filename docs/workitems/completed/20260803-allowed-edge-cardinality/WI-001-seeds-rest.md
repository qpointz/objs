# WI-001 — Seeds + REST

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Seeds + REST  
**Status:** done  
**Depends on:** WI-000

## Goal

Expose cardinality on seed import/export and registry HTTP APIs using the confirmed wire values.

## Scope

- Parse and serialize `cardinality` in `AllowedEdgeRuleSeedHandler` (omit or default when absent)
- `EdgeRequest` and `EdgeRelationRequest` on `ObjsRegistryController` include `cardinality`
- PUT replace-schema-edges persists cardinality from relation drafts
- OpenAPI / examples reflect `UNSPECIFIED` / `1:1` / `1:*`
- Controller and seed importer tests

## Out of scope

- Schema UI (WI-002)
- Design doc final pass beyond REST notes if already covered (WI-003 owns graph docs)

## Acceptance

- [x] Seed YAML without `cardinality` imports as `UNSPECIFIED`
- [x] Seed export emits the field when non-default (or always — pick one deterministic rule and test it)
- [x] Registry GET/PUT and schema-edge replace round-trip cardinality
- [x] Invalid wire values are rejected with a clear error
