# WI-001 — NamedGraphStore.exists

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-000  
**Gaps:** G-E1, G-E2, G-E5 (**locked**; implemented here); G-E3 stays deferred  

## Goal

Expose cheap graph-existence checks on [`NamedGraphStore`](../../../../objs-persistence/src/main/kotlin/org/poc/objs/core/persistence/NamedGraphStore.kt) so callers stop using `get(id) != null`.

```kotlin
fun exists(id: UUID): Boolean
fun exists(matcher: Matcher): Boolean
```

## Deliverables

- [x] `exists(id)` — `uow.read { graphDao.existsById(id) }`; no resolve (**G-E1**)
- [x] `exists(matcher)` — any match; **early exit** on first hit (**G-E2**); header path only
- [x] Matcher kinds per **G-E5**: `GraphExprMatcher` / `GraphIdsMatcher` / `AllGraphsMatcher`; empty `GraphIdsMatcher` → `false`; unsupported → fail fast
- [x] `requireGraphExists` shares the same header check (`graphDao.existsById` inside UoW)
- [x] Tests in `NamedGraphStoreTest`

## Out of scope

- REST HEAD / exists endpoint (G-E3)
- Compound `exists(id, matcher)`
- Changing `get` / `matchingHeaders` public semantics (may reuse internals)
