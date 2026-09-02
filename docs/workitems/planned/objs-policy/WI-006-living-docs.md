# WI-006 — Living docs and cross-links

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 6 — Living docs  
**Status:** planned  
**Depends on:** WI-004 (and WI-005 if not deferred)  
**Examples:** **docs**

## Goal

Bring design and repo entry docs in line with what shipped; leave a clear cold-start path for future engines (OPA) and consumers.

## Scope

- [ ] [`docs/design/policy/`](../../../design/policy/) reflects shipped modules/APIs
- [ ] [`fragments-and-analysis.md`](../../../design/graph/fragments-and-analysis.md) consumer list includes policy
- [ ] [`apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md) updated (planned → shipped wording)
- [ ] [`AGENTS.md`](../../../../AGENTS.md) module list includes `objs-policy-*` when modules exist
- [ ] [`EXAMPLES.md`](EXAMPLES.md) / [`GAPS.md`](GAPS.md) final pass (no stale `open` that should be closed)
- [ ] [`STORY.md`](STORY.md) acceptance checkboxes updated for completed work (do **not** move to `completed/` until user asks)

## Out of scope

- New features
- Story closure / archive

## Acceptance

- [ ] Cold-start links from STORY still work
- [ ] No doc claims OPA or workbench UI shipped unless they did
