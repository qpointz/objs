# WI-006 — Tests + docs pass

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Verify  
**Status:** done (automated); manual checklist remains for user  
**Depends on:** WI-003, WI-004, WI-005, WI-007, WI-008  
**Modules:** `:objs-service` UI, docs

## Goal

Fix UI tests; ensure `ui.md` matches STORY; run STORY Stage 5 manual checklist (especially always-visible explore scope + mode gating).

## Acceptance

- [x] `npm test` / `vitest run` in `objs-service/ui` green (93 tests)
- [ ] Manual checklist in STORY checked with user
- [x] Explorer Explore-scope keeps mode + summary visible (implemented WI-002/003)
