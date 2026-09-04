# Examples & scenarios — policy-evaluate-core (S1)

Motivating only. **Not** foundation requirements. Product DRL/selectors stay in apps.

**Call sequences (implement + document against):** [`evaluation-sequences.md`](../../../design/policy/evaluation-sequences.md)

Suite roll-up, seeds, batch matrix, and Drools scenarios live under follow-up stories.

---

## E1 — Postgres version gate (applicability)

**Policy intent (product):** “PostgreSQL must be version higher than 16.5.”

**Fragment A** — Postgres 16.4 present.  
→ Applicable → **FAIL** + finding on component entity.

**Fragment B** — libraries only.  
→ **`NOT_APPLICABLE`** (not pass, not fail).

**Fragment C** — forbidden relation.  
→ Finding may bind entities and/or edges; empty bindings valid for policy-level notes.

**Fragment D** — “must have ≥2 approvers” but none in graph.  
→ **FAIL** with **no finding** is valid. Finding checks are **soft** only (never invalidate the outcome).

**S1 lesson:** findings optional even on FAIL; soft validation only; outcomes must record **which policy version** ran.

---

## E2 — Flat collection over one fragment

1. Load policies into in-memory repository.  
2. Resolve fragment via `GraphFragmentPolicy`.  
3. Run `PolicyContextWirer` chain if registered.  
4. Applicability gate (bound into evaluate).  
5. CUSTOM (later Drools) evaluates in-scope only.  
6. Show findings + N/A list.

**S1 lesson:** orchestration is generic; org packs are not.

---

## E3 — Full graph vs selection

Same policies, two fragments: full graph vs Explorer subset → different applicability.

**S1 lesson:** applicability is a function of fragment content.

---

## Moved to follow-ups

| Scenario | Story |
|----------|--------|
| Suite hierarchy + folder roll-up (former E6) | [policy-suites](../../planned/policy-suites/STORY.md) |
| Seeds / MERGE packs | [policy-seeds-persistence](../../planned/policy-seeds-persistence/STORY.md) |
| Portfolio × suite matrix / batch (former E8) | [policy-batch](../../planned/policy-batch/STORY.md) |
| Drools fixture engine | [policy-drools](../../in-progress/policy-drools/STORY.md) |
| Workbench Policy play UI | [policy-workbench](../../planned/policy-workbench/STORY.md) |
| SBOM / extra REST consumer | [policy-consumer](../../planned/policy-consumer/STORY.md) |
