# WI-003 — ValidationIssue API + emitters

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-002 (GAPS closed)  

## Goal

Extend `ValidationIssue` with structured `subject` + `schema`. Populate at graph persist-gate emit sites.

## Deliverables

- [x] API types: `ValidationSubject`, `ValidationSchemaRef`, loci / kinds, `AllowedEdgeKey`
- [x] `Validator` + store membership / identity issues populate subject + schema
- [x] networknt instance location → JSON Pointer `schema.fieldPath`
- [x] Unit tests (`ValidatorTest`)
- [x] `path` remains optional debug only

## Out of scope

- Workbench TS (WI-004)
- Full seed/registry retrofit
