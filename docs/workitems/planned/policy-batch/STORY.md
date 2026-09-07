# Story: policy-batch — thin batch / result pack

**Slug:** `policy-batch`  
**Branch:** (not started)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/policy-batch/`](.)  
**Backlog:** [C-29](../../BACKLOG.md)  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-32--normative-order) step **8 / 9**  
**Before:** [C-33 `policy-results-persistence`](../../completed/20260907-policy-results-persistence/STORY.md); suite batch target needs [C-27](../../completed/20260905-policy-suites/STORY.md)  
**Next:** [C-30 `policy-consumer`](../policy-consumer/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/policy/overview.md`](../../../design/policy/overview.md)

## Goal

**Advanced / post-ready** mass assessment: thin batch layer runs policy set(s) against fragments from a **Fragments factory** (caller-owned fragment production) and a **Policies source** (suite-expanded or custom policy set) → packed per-subject results. **No** portfolio×suite matrix in foundation.

Primary programmatic API for continuous / large executions — after result persistence (C-33) wires evaluate → store.

## Work Items

- [ ] WI-000 — Story scaffold
- [ ] WI-001 — Design lock (batch GAPS)
- [ ] WI-002 — Batch API + sequential impl + tests
- [ ] WI-003 — Living docs

## Out of scope

- Matrix UI, cross-subject roll-up, REST (C-30)
- Result store (C-33)
