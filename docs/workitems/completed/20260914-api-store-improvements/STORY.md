# Story: api-store-improvements — store / validation API gaps

**Slug:** `api-store-improvements`  
**Branch:** `api-store-improvements`  
**Status:** completed  
**Closed:** 2026-09-14  
**Folder:** [`docs/workitems/completed/20260914-api-store-improvements/`](.)  
**Backlog:** [C-38](../../BACKLOG.md)  
**Base:** `origin/dev`  
**MR:** https://gitlab.qpointz.io/sandbox/bom-poc/-/merge_requests/70  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/graph/persist-sketch.md`](../../../design/graph/persist-sketch.md) · validation contracts in `:objs-api`  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

## Goal

Ship small, consumer-facing **store and validation API** improvements that unblock programmatic callers without waiting on larger epics (C-20 / C-30 / C-35):

1. **Graph existence** — `NamedGraphStore.exists(id)` and `exists(matcher)` so callers do not load a full `ResolvedGraph` via `get(id) != null`.
2. **Default deep version** — restore a no-`at` `createDeepGraphVersion` overload (`Instant.now()`); keep explicit `at: Instant` for backdated freezes (C-36).
3. **Structured `ValidationIssue`** — addressable `subject` + `schema` (redundant index / id / document OK) so clients never parse `path`.
4. **Workbench chrome** — graph context bar Open must not be overlapped by selector (Note1).

## Work Items

- [x] WI-000 — Story scaffold (`WI-000-story-scaffold.md`)
- [x] WI-001 — `NamedGraphStore.exists(id)` + `exists(matcher)` (`WI-001-named-graph-exists.md`)
- [x] WI-006 — `createDeepGraphVersion` default overload (`WI-006-create-deep-version-default.md`)
- [x] WI-007 — Graph context bar Open layout (Note1) (`WI-007-graph-context-open-layout.md`)
- [x] WI-002 — Design lock: ValidationIssue GAPS (`WI-002-validation-issue-design-lock.md`)
- [x] WI-003 — ValidationIssue API + Validator/store emitters + tests (`WI-003-validation-issue-api.md`)
- [x] WI-004 — Workbench TS / drop path-regex targeting (`WI-004-workbench-structured-issues.md`)
- [x] WI-005 — Living docs (`WI-005-living-docs.md`)

## Out of scope

- Changing validation *rules* or persist-gate order
- Retrofitting every seed/registry `ValidationIssue` in v1
- REST `HEAD` / exists endpoint (follow-up unless requested)
- Policy-batch / consumer (C-29 done; C-30 superseded)
- Store text search (C-20)
