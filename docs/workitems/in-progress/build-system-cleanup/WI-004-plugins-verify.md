# WI-004 — Minimize plugins + verify build

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Platform + prune  
**Status:** pending  
**Depends on:** WI-003

## Goal

Apply plugin keep set from inventory: drop Spring DM (already gone), unused catalog
plugins, optional root `apply false` cleanup, and resolve `jsonschema2pojo` per lock.
Verify `./gradlew build` (and core `testIT` if practical).

## Acceptance

- [ ] Plugin set matches STORY target table  
- [ ] `./gradlew build` passes  
- [ ] Story tracker updated  
