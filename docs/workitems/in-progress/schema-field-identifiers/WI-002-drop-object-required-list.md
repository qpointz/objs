# WI-002 — Drop OBJECT-level `required` list

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Required cleanup  
**Status:** done  
**Depends on:** WI-001  
**Modules:** `:objs-core`, `:objs-sbom-example`, UI types, tests

## Goal

Remove redundant OBJECT-node `required: List<String>` from the authoritative DSL. Field-level `BoMSchemaField.required` remains the only way to mark required properties.

## Acceptance

- [x] Domain model has no OBJECT `required` list
- [x] Seeds/fixtures stripped of OBJECT `required` lists **and** of `labels` / `attributes` fields
- [x] `SbomRegistry` + typed models + SBOM design docs agree (no labels/attributes)
- [x] JSON Schema projection still emits `"required"` from field flags
- [x] Core + relevant UI / example tests green
