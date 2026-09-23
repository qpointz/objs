# WI-003 — Lane B hooks + SBOM evidence/examine demo

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Codegen + fixture  
**Status:** done  
**Depends on:** WI-002  
**Examples:** **sbom** (G-30)

## Goal

SBOM: notable `Component` 1.0→2.0, hand ClassToClass step, fingerprint/BOM **evidence + examine** dual representation, regression tests. Closes C-23 G-30 for the SBOM vehicle.

## Deliverables

- [x] Component@1.0.0 + @2.0.0 in `SbomRegistry` / typed models
- [x] Hand `Component_1_0_0_to_2_0_0` (Lane B I/O = hand payload types; package `…migration`)
- [x] Fingerprint GET: `representation=saved|latest|both` (default **both**)
- [x] `AssetView.latestSchemaVersion` / `latestPayload` for examine projection
- [x] Regression tests (`ComponentUpgradeRegressionTest`)
- [x] C-23 G-30 addressed (SBOM evolved Component + dual wire)
- [ ] Full second snapshot JSON Schema export for generic Lane B codegen — **deferred** (SBOM demo uses hand DTOs matching B1 naming intent)

## Out of scope

- Persist rewrite (G-E6), edge-property L2 (G-E5), G-X8
