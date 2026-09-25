# WI-001 — Design lock

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — Design lock  
**Status:** completed  
**Depends on:** WI-000  
**Examples:** **docs**

## Goal

Confirm G-W* locks and publish thin contract pointers in living design docs.

## Done

- [x] [`GAPS.md`](GAPS.md) open section empty; G-W1…G-W8 / G-W7a resolved
- [x] [`STORY.md`](STORY.md) normative matches locks
- [x] Recipes filename: **`typed-conversion-recipes.md`** (created in WI-005)
- [x] [`EXAMPLES.md`](EXAMPLES.md) = codegen prove + SBOM/AR smoke
- [x] Contract notes in [`codegen-and-builder.md`](../../../design/graph/codegen-and-builder.md), [`api-and-codegen.md`](../../../design/graph/api-and-codegen.md), [`README.md`](../../../design/graph/README.md)

## Locked summary

| Id | Lock |
|----|------|
| G-W1 | `EntityCatalog` same package |
| G-W2 | Full conversion surface |
| G-W3 | Exact `Class` match only |
| G-W4 | Lane A; latest wins; optional C-35 upgrade couple |
| G-W5 | Separate `EdgeCatalog` |
| G-W6 | Both builder `add`s (WI-003 **in**) |
| G-W7 / G-W7a | Codegen prove; SBOM/AR smoke; recipes doc |
| G-W8 | Catalog misuse → `IllegalArgumentException` |
