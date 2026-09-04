# Gaps — policy-suites (C-27)

Close in this story’s WI-001 only. Assumes C-24 flat evaluate shipped.

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P26s | Suite model shape | **open** | Suite + node tree; fields; cycles forbidden |
| G-P27s | M:N membership | **open** | On suite nodes; sort order; **required**/enablement here (not on Policy — C-24 G-P3); membership ref = policy name + **`latest` or specific serial version** |
| G-P28s | Execute scope | **open** | Whole suite vs level/subtree |
| G-P29s | Folder roll-up formula | **open** | ERROR/FAIL/N/A/empty folder |
| G-P30s | Dedupe when M:N | **open** | Evaluate once; attach to placements |
| G-P31s | SuiteRepository vs PolicyRepository | **open** | Facade vs split |
| G-P32s | Suite versioning | **open** | Aligned with policy or independent |
| G-P33 | Suite / node applicability | **open** | Per-policy only vs node gates |
| G-P11s | Suite result shape | **open** | Per-node roll-up is **suite/app logic** (C-24: failed policy ≠ failed suite by default). Still cite executed policy version on leaf outcomes |
| G-P15s | `evaluateSuite` entry | **open** | Prefer **wrapper** over C-24 fixed `evaluate(fragment, policyRefs)` (G-P15); resolve suite → policy refs then call core |

## Inherited from C-24 (do not reopen)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P3 | Policy versioning | **resolved** (C-24) | Serial version on create/update; no policy `enabled`; suite refs `latest` \| version; results trace to version |

## Philosophy (inherited)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P36 | Suites first-class | **resolved** (intent) | Hierarchy + M:N + roll-up |
| G-P42 | Suite ≠ SBOM Portfolio | **resolved** | Shape analogy only |

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| — | — | — | — |
