# Story: policy-batch — thin batch / result pack

**Slug:** `policy-batch`  
**Branch:** (not started)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/policy-batch/`](.)  
**Backlog:** [C-29](../../BACKLOG.md)  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-31--normative-order) step **6 / 7**  
**Before:** [C-28 `policy-seeds-persistence`](../policy-seeds-persistence/STORY.md); suite batch target needs [C-27](../policy-suites/STORY.md)  
**Next:** [C-30 `policy-consumer`](../policy-consumer/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/policy/overview.md`](../../../design/policy/overview.md)

## Goal

Thin `evaluateBatch`: many opaque `{ subjectKey, fragment }` × one policy collection or suite → packed per-subject results. **No** portfolio×suite matrix in foundation.

## Work Items

- [ ] WI-000 — Story scaffold
- [ ] WI-001 — Design lock (batch GAPS)
- [ ] WI-002 — Batch API + sequential impl + tests
- [ ] WI-003 — Living docs

## Out of scope

- Matrix UI, cross-subject roll-up, REST (C-30)
