# Story: policy-consumer — optional REST / example consumer

**Slug:** `policy-consumer`  
**Branch:** (not started)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/policy-consumer/`](.)  
**Backlog:** [C-30](../../BACKLOG.md)  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-32--normative-order) step **8 / 8**  
**Before:** [C-29 `policy-batch`](../policy-batch/STORY.md) (preferred full stack); hard require [C-24](../../completed/20260904-policy-evaluate-core/STORY.md)  
**Next:** — (end of policy family)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/policy/overview.md`](../../../design/policy/overview.md)

## Goal

Optional **example-app** and/or extra REST consumer beyond the workbench play UI. Workbench tactical UI is **C-31** (`policy-workbench`) — not this story.

Never put policy modules on `:objs-service` by default. Product policies/suites stay in the app.

## Work Items

- [ ] WI-000 — Story scaffold
- [ ] WI-001 — Design lock (REST vs SBOM/example scope — no workbench)
- [ ] WI-002 — Chosen consumer path
- [ ] WI-003 — Living docs

## Out of scope

- Workbench Policy play UI (**C-31**)
- Matrix product UI
- Regulatory content in foundation
