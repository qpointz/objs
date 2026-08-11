# WI-005 — Identifier immutability on update

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 4 — Immutable updates  
**Status:** done  
**Depends on:** WI-004  
**Modules:** `:objs-core`, `:objs-service`, UI instance forms

## Goal

Identifier field values may be set when **creating** an entity or edge; on **update**, any change to the identity projection must fail. Align Composer edit forms with the API. See [`GAPS.md`](GAPS.md) G-1…G-3, G-5, G-6, G-10.

## Implementation

- [`BoMValidator.validateIdentifierImmutability`](../../../../objs-core/src/main/kotlin/org/poc/objs/core/validation/BoMValidator.kt) (+ entity/edge helpers)
- Wired in [`BoMGraphStore.validateMutation`](../../../../objs-core/src/main/kotlin/org/poc/objs/core/persistence/BoMGraphStore.kt) and [`BoMNamedGraphStore.validateMutate`](../../../../objs-core/src/main/kotlin/org/poc/objs/core/persistence/BoMNamedGraphStore.kt)
- UI: `lockIdentifiers` on [`SchemaInstanceForm`](../../../../objs-service/ui/src/SchemaInstanceForm.tsx) / `PayloadInspector`; Composer uses `draftState.baselineEntityIds` / `baselineEdgeIds` as `persistedIds` (G-3)

## Acceptance

- [x] Update with changed identifier values rejected on entity write paths (tests)
- [x] Create with identifier values succeeds; non-identity field updates succeed
- [x] Edit form locks identifier fields for baseline (persisted) ids
- [x] Edge property path included in validateMutate (G-1)
