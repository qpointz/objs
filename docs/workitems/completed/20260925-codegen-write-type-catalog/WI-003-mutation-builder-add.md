# WI-003 — Generic mutation builder `add`

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Builder  
**Status:** completed  
**Depends on:** WI-002  
**Examples:** **—** (codegen module tests; consumer smoke in WI-004)

## Goal

Add generic entity registration on generated `GraphMutationBuilder` so callers need not use per-type `addProduct` / `addDataset` for simple sets (G-W6: **both** overloads).

## Done

- [x] `builder.add(datasetPojo)` matches `addDataset(datasetPojo)` entity set for a fixture type
- [x] `builder.add(node)` / `add(typed)` register the same entity id/type
- [x] Unknown payload fails closed with IAE
- [x] `./gradlew :objs-codegen-java:test`
