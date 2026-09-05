# Gaps — policy-suites (C-27)

Close in this story’s **WI-001** only. Assumes C-24 flat evaluate shipped.

**Draft for review:** [`docs/design/policy/suites.md`](../../../../design/policy/suites.md). Resolve gaps **individually** (WI-001); notes point at the draft.

| #      | Topic                               | Status       | Notes (draft — review)                                                                                                                                                                                                                                                                                  |
| ------ | ----------------------------------- | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| G-P26s | Suite model shape                   | **resolved** | Suite + **folder** hierarchy is a **tree** (**DAG**, single parent); **cycles forbidden**; folder tags/annotations; suite **`rollUpStrategyKind`** (engine); folder **`rollUpMode`** + optional `severityConfig`; participation **ENABLED** \| **DISABLED** (no eval, non-voting) \| **IGNORED** (eval, non-voting) — see suites.md |
| G-P27s | Policy assignment (matchers)        | **resolved** | Extensible SPI; **STATIC_LIST** (`policyId`=Policy.id; version string omit=latest \| `major.minor.serial` \| `major.*`; missing→**ERROR** or skip); **METADATA** (slug category?, tags all, annotations all; ≥1; AND); include/exclude; children first; `policyId` uniq per step — see suites.md |
| G-P28s | Execute scope                       | **resolved** | **full** suite \| single **subfolder** (subtree) \| **set of subfolders** \| **subset of policies** — see suites.md                                                                                                                                                                                     |
| G-P29s | Roll-up strategy                    | **resolved** | Suite selects **engine** (`BUILTIN`; future `DROOLS`); each folder supplies **`rollUpMode`** ALL_PASS\|ANY_PASS (+ severityConfig) as **inputs**; strategy interprets outcomes; ENABLED votes; PASS+sev warning; ERROR→HIGH synthetic — see suites.md |

| G-P30s | Dedupe when reused                  | **resolved** | Suite taxonomy does **not** affect policy execution. After selection, run via **`ExecutionStrategy`** (dedupe vs non-dedupe defined there). Reusable by **C-29** batch (batch → one or more execution strategies → multiple fragments) — see suites.md                                                  |
| G-P31s | SuiteRepository vs PolicyRepository | **resolved** | **Split** — dedicated `SuiteRepository` (does not own policies); same spirit as C-32 `CategoryRepository` — see suites.md                                                                                                                                                                               |
| G-P32s | Suite versioning                    | **resolved** | **No** suite versioning for CRUD. Regulatory need: persisted executions carry **full config** for **replay + inspect**. **Not** a first-class API concept in C-27 — see suites.md                                                                                                                       |
| G-P33  | Suite / node applicability          | **resolved** | **Per-policy only** (no suite/folder gates). N/A policy → **no results**, **no voting** (same participation as folder **DISABLED**: no execution, no results, no vote) — see suites.md                                                                                                                  |
| G-P11s | Suite result shape                  | **resolved** | `evaluationId` + `meta` (incl. **tags/annotations**) + reporting `tree` (refs → `outcomes`) + flat `outcomes`. C-27: **no input persist**; fragment/graph freeze deferred — see suites.md · RESULTS-MODEL.md |
| G-P15s | `evaluateSuite` entry               | **resolved** | **Wrapper** over C-24 `evaluate(fragment, policyRefs)` — expand suite → policies/refs, call core, then roll-up; flat contract unchanged — see suites.md                                                                                                                                                 |

## Inherited from C-24 (do not reopen)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P3 | Policy versioning | **resolved** (C-24) | Serial on create/update; no policy `enabled`; pin latest \| serial; results trace to serial. Suite matchers treat **`Policy.id` as stable `policyId`** across serial bumps (G-P27s) |

## Philosophy (inherited)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P36 | Suites first-class | **resolved** (intent) | Hierarchy + assignment + roll-up |
| G-P42 | Suite ≠ SBOM Portfolio | **resolved** | Shape analogy only |

## Decision log

| # | Decision | Date | Summary |
|---|----------|------|---------|
| G-P26s | Suite / folder shape | 2026-09-05 | Tree (DAG, single parent); cycles forbidden; folder tags + annotations; suite selects roll-up strategy kind; folder severityConfig; participation ENABLED \| DISABLED (no eval) \| IGNORED (eval, non-voting) |
| G-P28s | Execute scope | 2026-09-05 | full suite \| one subfolder (subtree) \| set of subfolders \| subset of policies |
| G-P29s | Roll-up strategy | 2026-09-05 | *(superseded 2026-09-05 corrective)* Initially locked ALL_PASS/ANY_PASS as suite strategy kinds |
| G-P29s | Roll-up strategy (corrective) | 2026-09-05 | Suite `rollUpStrategyKind` = engine (`BUILTIN`; future `DROOLS`). Folder `rollUpMode` = ALL_PASS\|ANY_PASS (+ severityConfig) as inputs the engine interprets. SPI `rollUp(folder, votingChildren)` unchanged |

| G-P31s | Repositories | 2026-09-05 | Split SuiteRepository from PolicyRepository (suite does not own policies) |
| G-P32s | Suite versioning / replay | 2026-09-05 | No suite versioning. Regulatory: full config on persisted execution for replay + inspect; not first-class API in C-27; replay-only (need not keep all historical versions in live tables) |
| G-P33 | Applicability | 2026-09-05 | Policy-level only. N/A → no results, no voting (same as DISABLED: no execution, no results, no vote). No suite/folder applicability gates |
| G-P15s | evaluateSuite entry | 2026-09-05 | Wrapper over flat evaluate(fragment, policyRefs); expand suite then roll-up; C-24 contract unchanged |
| G-P30s | Execution / dedupe | 2026-09-05 | Suite = policy selection + taxonomy/roll-up only. Execution (+ dedupe\|non-dedupe) = ExecutionStrategy, independent of suite tree; reusable by C-29 batch → strategy(ies) → fragments |
| G-P11s | Suite result shape | 2026-09-05 | meta + tree (refs to outcomes) + outcomes; evaluationId central; evaluation-level tags/annotations; C-27 no-input-persist only; graph freeze / snapshot later |
| G-P27s | Matchers | 2026-09-05 | Extensible SPI; STATIC_LIST + METADATA; policyId=Policy.id; version match string; missing→ERROR or skip; category by slug; criteria AND |
