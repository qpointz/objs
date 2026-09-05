# Story: policy-suites — suite hierarchy + folder roll-up

**Slug:** `policy-suites`  
**Branch:** (not started)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/policy-suites/`](.)  
**Backlog:** [C-27](../../BACKLOG.md)  
**Sequence:** [policy family](../../SEQUENCE.md#policy-family-c-24c-32--normative-order) step **5 / 8**  
**Before:** [C-32 `policy-metadata`](../../completed/20260905-policy-metadata/STORY.md); hard require [C-24](../../completed/20260904-policy-evaluate-core/STORY.md) (+ C-26 for Drools); C-31 workbench shipped  
**Next:** [C-28 `policy-seeds-persistence`](../policy-seeds-persistence/STORY.md)  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Design:** [`docs/design/policy/overview.md`](../../../design/policy/overview.md)

## Goal

Add PolicySuite trees (folders), **M:N** policy membership, `evaluateSuite`, and **folder roll-up** into suite results. Membership may pin policy **`latest`** or a **specific serial version** (C-24 G-P3); enablement/required lives on suite membership — not on Policy. Testable with CUSTOM engine if Drools is delayed; preferred order still runs after C-26.

## Work Items

- [ ] WI-000 — Story scaffold
- [ ] WI-001 — Design lock (suite GAPS)
- [ ] WI-002 — Suite model + repository APIs
- [ ] WI-003 — Suite execution + roll-up tests
- [ ] WI-004 — Living docs

## Out of scope

- Seeds (C-28), batch matrix (C-29), REST (C-30), product suite content
