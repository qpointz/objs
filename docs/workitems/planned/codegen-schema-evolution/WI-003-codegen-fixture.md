# WI-003 — Lane B codegen hooks + evolved-snapshot fixture

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Codegen + fixture  
**Status:** planned (blocked on WI-002)  
**Depends on:** WI-002  
**Examples:** **codegen** (G-30-style)

## Goal

Wire export/generator support for Lane B snapshot DTOs per locked G-E1/G-E2/G-E13. Keep Lane A latest-only for object-model bindings. Prove hand migration + typed read in an evolved-snapshot consumer fixture (close C-23 G-30). Ensure regenerating generated sources does not touch hand migration package.

## Deliverables

- [ ] Export / Gradle inputs for Lane B per design lock
- [ ] `objs-codegen-java` (or build docs) ignores Lane B for OM node emission; ReadView accepts injected registry
- [ ] Example: Product@1 + Product@2, hand `UpgradeStep`, stored @1 reads as latest typed
- [ ] Regeneration safety documented (and smoke-checked)
- [ ] C-23 G-30 addressed

## Out of scope

- Persisting rewritten pins (unless G-E6 in scope)
- Per-version object-model APIs
