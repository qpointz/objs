# Story: objs-policy — foundation policy evaluation

**Slug:** `objs-policy`  
**Branch:** (not started — **planned** only; no production modules until design-lock closes open GAPS)  
**Status:** planned  
**Folder:** [`docs/workitems/planned/objs-policy/`](.)  
**Backlog:** [C-24](../../BACKLOG.md)  
**Base:** `origin/dev`  
**Depends on:** `GraphFragment` / `ResolvedGraphFragment` / `GraphFragmentPolicy` (shipped — [`graph-frontend-jgrapht`](../../completed/20260903-graph-frontend-jgrapht/STORY.md) / [`docs/design/graph/fragments-and-analysis.md`](../../../design/graph/fragments-and-analysis.md))  
**Does not block:** C-20 store text search  
 
**Design (draft until WI-001):** [`docs/design/policy/overview.md`](../../../design/policy/overview.md) ([index](../../../design/policy/README.md))  
**Gaps:** [`GAPS.md`](GAPS.md)  
**Examples / scenarios:** [`EXAMPLES.md`](EXAMPLES.md)  
**Process:** [`docs/workitems/RULES.md`](../../RULES.md)

---

## Cold start (read this first)

If you are new to this story, read in this order:

1. **This section + Goal + Glossary** — what we are building and what words mean.
2. **[`docs/design/policy/overview.md`](../../../design/policy/overview.md)** — detailed design draft with diagrams.
3. **[`EXAMPLES.md`](EXAMPLES.md)** — Postgres / suite roll-up / SBOM-shaped scenarios (motivating only).
4. **Pipeline + Locked architecture (provisional)** — normative flow including **applicability** and **suite execution**.
5. **[`GAPS.md`](GAPS.md)** — every decision still open; **WI-001 must close `open` rows before code**.
6. **[`docs/design/graph/fragments-and-analysis.md`](../../../design/graph/fragments-and-analysis.md)** — how fragments already feed Gremlin/JGraphT (same input class).
7. **[`docs/design/graph/apps-vs-foundation.md`](../../../design/graph/apps-vs-foundation.md)** — foundation vs example apps.
8. **WI files in order** — scaffold → design-lock → api → core → drools → optional consumer → docs.

**Do not** start WI-002+ Kotlin/Gradle work until WI-001 marks every `open` GAPS row `resolved` or explicitly `deferred`.

---

## Goal

Design, then ship, a **foundation policy subsystem** (`objs-policy-*`) that:

1. Stores **executable policy artefacts** (engine-agnostic data model + repository) — not product regulations.
2. Organizes policies into **Policy Suites**: hierarchical trees (folders / nodes), with **many-to-many** membership (one policy in many suites; one suite node holds many policies) — portfolio-like taxonomy, but for policies.
3. Accepts any **`GraphFragment`** (full graph, matcher selection, multi-graph union, …).
4. Optionally **enriches** the fragment (implementer SPI).
5. **Selects applicable policies** for that fragment (implementer-defined applicability — first-class).
6. **Evaluates** only applicable policies via pluggable engines (first engine: **Drools**).
7. Returns a **unified, engine-agnostic evaluation result**, including **suite folder roll-up** (status propagated from leaves to parent folders / suite root).
8. Surfaces **findings**: each finding may reference **0 or more** fragment **entities (nodes)** and **0 or more** fragment **edges** (evidence bindings).
9. Ships a **seed format** for policies and suites (portable YAML interchange, same family as graph config seeds) so orgs can author and load policy packs without hard-coding them in foundation jars.
10. Offers a **thin batch / result-pack** API: many opaque `subjectKey` + fragment pairs × one suite or policy target → packed list of per-subject results — enough for product multi-dimensional views **without** owning portfolio×suite matrices.

**Naming:** the module family is **`objs-policy`**, not `objs-assessment`. Assessment is an optional product word for a *run* or *result*; the foundation noun is **policy**.

**SBOM is only a motivating example:** an application graph should be assessable against user-defined IT-security / governance **suites**. Concrete PCI/MiFID/Postgres rules, Application/Portfolio binding, and **portfolio × suite assessment matrices** stay in examples/apps — never hard-coded in objs-\*. Suite hierarchy is analogous to SBOM **portfolios / subject areas**, but lives in the policy store (not SBOM domain tables).

---

## Glossary

| Term | Meaning in this story |
|------|------------------------|
| **Policy** | Foundation artefact: metadata + `engineKind` + evaluation body (+ optional applicability artefact). |
| **Policy suite** | Named hierarchical taxonomy of **suite nodes** (folders) that **reference** policies. Analogous to SBOM portfolio + subject-area tree, but for policies. |
| **Suite node** | Folder / subject-area node in a suite tree (root or child). May contain child nodes and/or policy memberships. |
| **Suite membership** | **Many-to-many**: a policy may belong to many suite nodes (across one or many suites); a suite node may list many policies. |
| **Policy repository** | Dedicated store for policies **and** suites (not the entity pool / named-graph membership). |
| **Graph fragment** | `(entities, edges)` — see `GraphFragment` / `ResolvedGraphFragment` in `:objs-api`. |
| **GraphFragmentPolicy** | Existing **normalization** (identity, duplicates, dangling ends). **Not** an executable business policy. |
| **Enrichment** | Optional transform/view over a resolved fragment before applicability/evaluation (SPI). |
| **Applicability** | Whether a policy is **in scope** for this fragment. Separate from pass/fail. |
| **Evaluation** | Running the policy body against the (enriched) fragment for **applicable** policies only. |
| **Suite execution** | Evaluate the policy set implied by a suite (or suite subtree / level), then **roll up** statuses to folder nodes and suite root. |
| **Folder / node roll-up** | Aggregate child node statuses + member policy outcomes into a parent node status (e.g. IT Governance fails if API fails). |
| **Finding** | Discrete evaluation message (severity, text, policy/rule refs). A policy outcome may carry **0..n findings**. |
| **Evidence binding** | On a finding (or outcome): **0..n entity ids** and **0..n edge ids** from the evaluated fragment. Empty lists are valid (policy-level / informational with no graph locus). |
| **Policy / suite seeds** | Portable multi-document YAML (or locked equivalent) to author and MERGE/upsert policies and suites — analogous to `ObjectSchema` / `Graph` seeds ([`seeds.md`](../../../design/graph/seeds.md)). |
| **PolicyEngine** | Adapter (Drools, later OPA, custom) that executes evaluation bodies. |
| **EvaluationResult** | Engine-agnostic outcome: per-policy status including `NOT_APPLICABLE`, **findings with evidence bindings**; for suite runs, also **per-node rolled-up status**. |
| **Batch / result pack** | Thin foundation facility: evaluate many `{ subjectKey, fragment }` subjects against one suite or policy collection; return an ordered bag of per-subject results. `subjectKey` is opaque (caller interprets). |
| **Assessment matrix** | Product view (e.g. portfolio apps × suite folders). **Not** foundation — built by composing batch packs + portfolio membership. |
| **SBOM `Policy` entity** | Domain ontology node (`COMPLIES_WITH`, etc.). **Not** the same as foundation Policy artefacts. |
| **SBOM Portfolio** | Product taxonomy of **applications**. Inspiration for suite tree UX/shape only — not the same store. |

---

## Why foundation (scope boundary)

Same citizen class as Gremlin / JGraphT: **fragment in → engine-agnostic DTOs out**. Unlike those engines, policy also owns an **artefact repository**.

| Foundation (`objs-policy-*`) owns | Foundation must **not** own |
|----------------------------------|-----------------------------|
| Policy data model (engine-agnostic) | Regulatory catalogs, PCI/MiFID text, “Application must…” product rules |
| **Policy suite** model (tree + **M:N** membership) + repository | Binding suites to SBOM Application / Portfolio / fingerprint |
| Policy repository (CRUD / version / resolve-by-ref) | Hard-coded suite trees (“IT Governance”) as product content |
| Enricher SPI + **ApplicabilitySelector SPI** + orchestration | Hard-coded “has Database” product predicates |
| Suite execution + **folder roll-up** into unified result | Scoring UX, approval workflows |
| **Thin batch / result pack** (opaque `subjectKey`s) | **Portfolio × suite assessment matrix**, multi-axis joins, cross-subject portfolio scores |
| **Seed format** for Policy + Suite (import/MERGE) | Concrete regulatory / “IT Governance” seed *content* in foundation jars |
| Unified `EvaluationResult` (policies + suite nodes + **findings ↔ 0..n nodes/edges**) | Replacing persist-gate schema validation |
| `objs-policy-drools` (first adapter) | Concrete regulatory DRL in foundation jars |

**Critical split:** foundation `Policy` ≠ SBOM seed type `Policy` / `COMPLIES_WITH`.  
**Critical analogy:** suite tree ≈ SBOM portfolio/subject-area **shape**; suite store ≠ SBOM portfolio tables.

---

## Pipeline (normative — provisional until WI-001)

### A — Flat policy collection (still supported)

```text
PolicyCollection (refs or loaded artefacts)
        │
        │     GraphFragment (select / union / builder / …)
        │              │
        │              v
        │     GraphFragmentPolicy.resolve   ← existing objs-api
        │              │
        │              v
        │     ResolvedGraphFragment
        │              │
        │              v
        │     FragmentEnricher(s)           ← optional SPI
        │              │
        └──────────────┤
                       v
         ApplicabilitySelector.select(fragment, policies)   ← implementer SPI
                       │
         ┌─────────────┴─────────────┐
         │ applicable[]              │ notApplicable[]
         v                           v
  PolicyEngine.evaluate(...)   record NOT_APPLICABLE (reason, policy ref)
         │
         v
  EvaluationResult (applied outcomes + N/A entries)
```

### B — Suite execution (hierarchy + roll-up)

```text
PolicySuite (or suite level / subtree)
        │
        v
 collect member policies (dedupe by policy id if M:N places same policy twice)
        │
        v
        … same resolve → enrich → applicability → evaluate as in (A) …
        │
        v
 roll up per-policy outcomes → suite nodes → suite root
        │
        v
 SuiteEvaluationResult
   - per-policy outcomes (with suite-node placement refs)
   - per-node status (folder roll-up)
   - root status
```

**Illustrative suite (product content — not foundation):**

```text
IT Governance                    → FAIL  (child API failed)
├── Database                     → OK    (applicable members passed / N/A ignored per lock)
│   ├── [ ] Mongo is up to date
│   └── [ ] do not use FoxPro
└── API                          → FAIL
    └── [ ] registered in Apigee
```

**Example:** evaluation body “Postgres version must be > 16.5” runs **only** when applicability says the fragment has a database/Postgres subject. No database → **`NOT_APPLICABLE`**, not pass and not fail. Parent folder roll-up **ignores N/A** for failure (exact aggregate formula in GAPS).

Applicability must **not** be buried only inside Drools `when` for the foundation contract (engines may still use when for efficiency). Callers need explicit N/A across Drools / OPA / custom.

**Suite applicability (provisional):** primary gate remains **per-policy**. Whether whole suites or suite *nodes* can be marked N/A for a fragment is an open GAPS item (`G-P33`).

### C — Thin batch / result pack (provisional)

```text
subjects: [ { subjectKey, fragment }, … ]   // subjectKey opaque
target: suiteId | policyRefs
        │
        v
 for each subject: same pipeline as (A) or (B)
        │
        v
 BatchEvaluationResult
   - items: [ { subjectKey, EvaluationResult | SuiteEvaluationResult }, … ]
   - optional pack-level diagnostics only (refused fragments, orchestration errors)
```

Helps product build multi-dimensional views (e.g. portfolio apps × suite). Foundation **does not** assemble the matrix, name axes, or roll up across subjects.

---

## Module map (provisional)

| Module | Role | Depends on (intent) |
|--------|------|---------------------|
| `:objs-policy-api` | Model, SPIs, result DTOs; Spring-free | `:objs-api` |
| `:objs-policy-core` | Repository + enrich → apply → evaluate orchestration; Spring-free preferred | `:objs-policy-api` |
| `:objs-policy-drools` | First `PolicyEngine` | `:objs-policy-api` (+ core as needed) |
| `:objs-policy-opa` | Later | — |
| `:objs-policy-service` | Optional Spring REST (like jgrapht-service) | Later / gated |

**Not** on `:objs-service` classpath by default (opt-in like `:objs-jgrapht-service`).

Exact Gradle wiring, package names, and whether repository JPA lives in core vs a persistence submodule = [`GAPS.md`](GAPS.md).

---

## Normative (provisional — lock in WI-001)

| Topic | Draft |
|-------|--------|
| Family name | `objs-policy` (not assessment) |
| Input | `GraphFragment` → resolve → `ResolvedGraphFragment` |
| Suites | Hierarchical suite nodes + **M:N** policy membership; suite execution with **folder roll-up** |
| Applicability | Explicit pipeline step + implementer `ApplicabilitySelector` SPI; optional per-policy applicability artefact |
| First engine | Drools in `:objs-policy-drools` |
| Policy / suite storage | Dedicated **PolicyRepository** (+ suite store), **not** graph pool entities |
| Seed format | **Required** — authorable seed docs for Policy and Suite (hierarchy + membership); product packs live in examples/apps, not foundation |
| Result | Per-policy `PASS` \| `FAIL` \| `ERROR` \| `NOT_APPLICABLE`; suite runs add **per-node rolled-up status**; N/A must not count as failure (aggregation in GAPS) |
| Findings | First-class; each finding binds **0..n entities** and **0..n edges** (UUIDs from the fragment); empty bindings allowed |
| Batch / result pack | Thin: many opaque `subjectKey` + fragment × one suite/policy target → packed per-subject results; **no** matrix |
| Product rules / suite trees / matrices | Examples/apps only (e.g. “IT Governance” content; portfolio × suite grid) |
| REST / workbench | Deferred unless WI-001 pulls optional service in |
| SBOM consumer | Optional late WI; example-owned policies/suites + selector |

---

## Stages

| Stage | WIs | Ready | Stop gate |
|-------|-----|-------|-----------|
| 0 — Scaffold | WI-000 | planned | Folder + backlog row exist |
| 1 — Design lock | WI-001 | after WI-000 | **No code until all open GAPS closed/deferred** |
| 2 — API contracts | WI-002 | after WI-001 | Interfaces compile; no Drools yet |
| 3 — Core orchestration + repository | WI-003 | after WI-002 | Stub/CUSTOM engine + stub applicability tests green |
| 4 — Drools adapter | WI-004 | after WI-003 | Fixture DRL only; no regulatory content in foundation |
| 5 — Optional consumer | WI-005 | after WI-004; **explicitly gated** | Example or REST proves flow; product policies stay in app |
| 6 — Living docs | WI-006 | after chosen consumer path | Design + AGENTS/apps-vs-foundation pointers current |

---

## Work Items

- [ ] WI-000 — Story scaffold — examples: **—** (`WI-000-story-scaffold.md`)
- [ ] WI-001 — Design lock (philosophy, SPIs, model, GAPS) — examples: **docs** (`WI-001-design-lock.md`)
- [ ] WI-002 — `:objs-policy-api` contracts — examples: **—** (`WI-002-policy-api.md`)
- [ ] WI-003 — `:objs-policy-core` repository + pipeline — examples: **—** (`WI-003-policy-core.md`)
- [ ] WI-004 — `:objs-policy-drools` adapter — examples: **—** (`WI-004-policy-drools.md`)
- [ ] WI-005 — Optional HTTP and/or example consumer — examples: **gated** (`WI-005-optional-consumer.md`)
- [ ] WI-006 — Living docs and cross-links — examples: **docs** (`WI-006-living-docs.md`)

---

## Out of scope

- Implementation of WI-002+ until the user starts this story **and** WI-001 closes open GAPS
- OPA/Rego adapter (follow-up story unless WI-001 schedules a stub only)
- Hard-coding product applicability or regulatory DRL in objs-\*
- Migrating SBOM ontology `Policy` nodes into the foundation repository
- Treating `NOT_APPLICABLE` as pass or fail
- Compliance scoring product, fingerprint approval workflows
- Replacing persist-gate validation ([`docs/design/graph/validation.md`](../../../design/graph/validation.md))
- Putting policy modules on `:objs-service` by default
- **Portfolio × suite assessment matrix** (or any multi-axis management grid) — product owns axes and layout; foundation only packs per-subject results

---

## Acceptance (story-level, after implementation stages)

- [ ] Open GAPS from WI-001 are **resolved** or explicitly **deferred**
- [ ] Caller can load a policy collection **or execute a suite**, pass a fragment, get applicable vs N/A vs evaluated outcomes in one unified result
- [ ] Suite run returns **per-node roll-up** (parent fails when a required child fails — per locked aggregate rules)
- [ ] Same policy can appear in multiple suite nodes / suites (M:N); evaluation dedupes sensibly (per lock)
- [ ] Findings may list **0..n entity ids** and **0..n edge ids**; fixtures prove both empty and multi-binding cases
- [ ] Policies and suites can be loaded from the locked **seed format** (round-trip or MERGE upsert in tests)
- [ ] Thin **batch/result pack** evaluates multiple opaque subjects and returns keyed per-subject results (no cross-subject roll-up)
- [ ] Drools evaluates fixture policies without foundation containing real regulatory text
- [ ] Foundation types never require SBOM/AR domain classes
- [ ] `./gradlew :objs-policy-api:test :objs-policy-core:test :objs-policy-drools:test` (and any opted-in service/example tests)

---

## Process notes

1. One WI at a time; `[x]` + one commit + push per WI ([`RULES.md`](../../RULES.md)).
2. Do not start WI-002 until WI-001 acceptance is met.
3. Branch from `origin/dev` as `objs-policy` when the user starts the story.
4. Do not close this story until the user asks.
