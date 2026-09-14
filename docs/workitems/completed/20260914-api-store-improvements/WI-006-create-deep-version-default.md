# WI-006 — createDeepGraphVersion default overload

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-000  
**Gaps:** G-E4  

## Goal

After C-36 backdated freeze, callers need a clear **default** freeze API again: omit `at` → stamp with `Instant.now()`. Keep an overload where **`at: Instant` is mandatory** for migrations.

## Deliverables

- [x] `NamedGraphStore.createDeepGraphVersion(graphId, versionAnnotations = emptyMap())` → `Instant.now()`
- [x] `NamedGraphStore.createDeepGraphVersion(graphId, versionAnnotations = emptyMap(), at: Instant)` — non-null `at`
- [x] Same pair (or equivalent) on `DeepGraphVersionService`; no `Instant?` “null means now”
- [x] REST / SBOM keep omitting `createdAt` → now behaviour
- [x] Tests: existing 1-/2-arg and 3-arg callers still compile; backdated path unchanged
- [x] Close G-E4 in [`GAPS.md`](GAPS.md)

## Out of scope

- Changing Option B backdate semantics (G-O*)
- Rewriting existing version rows
