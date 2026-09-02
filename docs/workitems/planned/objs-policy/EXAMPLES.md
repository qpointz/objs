# Examples & scenarios — objs-policy (motivating only)

These scenarios explain **why** the foundation exists. They are **not** foundation requirements and must not be hard-coded into `objs-policy-*`.

Concrete DRL/Rego, SBOM Application binding, and product applicability predicates belong in **example apps** (or later consumer WIs).

---

## E1 — Postgres version gate (applicability)

**Policy intent (product):** “PostgreSQL must be version higher than 16.5.”

**Fragment A** — application BOM includes a Database/Postgres component at 16.4.  
→ Applicability: **yes** (database present).  
→ Evaluation: **FAIL** with a finding (“version 16.4 < 16.5”) bound to **that component entity** (0 edges).  

**Fragment B** — application BOM is libraries only; no database.  
→ Applicability: **no**.  
→ Result entry: **`NOT_APPLICABLE`** (reason: no database subject); typically **no findings** (or informational only — per G-P12f).  
→ Must **not** fail the overall assessment solely because of this policy.

**Fragment C** — forbidden relation present (e.g. uses FoxPro).  
→ Finding may bind **0..n entities and 0..n edges** (e.g. the FoxPro component **and** the `USES` edge).  
→ Finding with **empty** entity/edge lists is valid for policy-level statements with no graph locus.

**Foundation lesson:** split **applicability** from **evaluation**; findings carry optional graph locus (nodes and/or edges).

---

## E2 — Policy collection over one Combined SBOM fragment

Operator selects an application’s Combined SBOM graph (or a matcher selection / multi-BOM union already resolved to `GraphFragment`).

1. Load policy collection from PolicyRepository (user-defined set for that org).
2. Resolve fragment via `GraphFragmentPolicy`.
3. Enrich if the app registered enrichers (e.g. derive “hasCriticalVuln” signals — app-owned).
4. ApplicabilitySelector (app-owned) partitions policies.
5. Drools (or later OPA) evaluates applicable only.
6. UI shows applied **findings** (highlight bound nodes/edges on canvas) + skipped (N/A) list.

**Foundation lesson:** orchestration is generic; SBOM UI and org policy packs are not.

---

## E3 — Full graph vs selection

Same policy collection, two fragments:

- **Full graph** — more subjects → more policies applicable.
- **Selection** (Explorer subset) — fewer subjects → more N/A.

**Foundation lesson:** applicability is a function of **fragment content**, not of graph header alone.

---

## E4 — Mixed engines (future)

Collection contains Drools + OPA policies. Orchestrator dispatches by `engineKind` after applicability.

**Foundation lesson:** unified result shape; engines are adapters.

---

---

## E6 — Policy suite hierarchy + folder roll-up

**Suite shape (product content — not foundation):**

```text
IT Governance
├── Database
│   ├── [policy] Mongo is up to date
│   └── [policy] do not use FoxPro
└── API
    └── [policy] registered in Apigee
```

**Membership:** M:N — e.g. “Mongo is up to date” may also sit under another suite (“Data Platform”). Evaluating a suite collects members (dedupe policy once per run — per G-P30s).

**Fragment:** application graph with Mongo OK, no FoxPro, Apigee registration missing.

| Node / policy | Outcome |
|---------------|---------|
| Mongo is up to date | PASS |
| do not use FoxPro | PASS (or N/A if no FoxPro subject — per selector) |
| Database (folder) | **OK** |
| registered in Apigee | FAIL |
| API (folder) | **FAIL** |
| IT Governance (root) | **FAIL** (API child failed) |

**Foundation lesson:** suite execution = collect → apply applicability → evaluate → **roll up to folders**. Flat `PolicyCollection` evaluate remains available without a suite.

**Analogy:** SBOM portfolio → subject areas → applications for MI scope; here suite → folders → **policies**. Do not store suites in SBOM portfolio tables.

---

## E7 — Seed format for policies and suites

Product teams author packs as multi-document YAML (envelope TBD in G-P34seed; draft sketch):

```yaml
apiVersion: objs.poc.org/v1
kind: Policy
name: mongo-up-to-date
version: "1"
engineKind: DROOLS
body: |
  # fixture DRL — product content, not foundation
---
apiVersion: objs.poc.org/v1
kind: PolicySuite
name: it-governance
nodes:
  - key: root
    name: IT Governance
    children: [database, api]
  - key: database
    name: Database
    policies: [mongo-up-to-date, no-foxpro]
  - key: api
    name: API
    policies: [registered-in-apigee]
```

**Foundation lesson:** seed **format + importer** are foundation; seed **files** with real rules live under examples/apps. MERGE upsert like graph seeds ([`seeds.md`](../../../design/graph/seeds.md)).

---

## E8 — Portfolio × suite matrix (product) via thin batch (foundation)

**Product intent:** portfolio owner selects a portfolio level (app set) and a policy suite (“IT Governance”). Management wants a matrix: apps × suite folders (or root status).

**Foundation role (thin):**

```text
batchEvaluateSuite(
  subjects = [
    { subjectKey: "app-A", fragment: combinedBomA },
    { subjectKey: "app-B", fragment: combinedBomB },
    …
  ],
  suiteId = it-governance
) → [
  { subjectKey: "app-A", suiteResult: { root: FAIL, nodes: …, findings: … } },
  { subjectKey: "app-B", suiteResult: { root: OK, … } },
]
```

**Product role:** resolve portfolio level → apps → fragments; call batch; **layout the matrix**; interpret `subjectKey` as application ids.

**Out of foundation:** grid axes, portfolio membership, cross-app scores, heatmaps.

---

## E5 — What is out of these examples

- Mapping SBOM ontology `Policy` nodes / `COMPLIES_WITH` edges into PolicyRepository.
- Storing suites as SBOM portfolios / subject areas.
- Building the **assessment matrix** inside objs-policy (product uses batch pack).
- MI report heuristics (`MiReportService`) as a PolicyEngine.
- Persist-gate JSON Schema validation as a policy evaluation.

---

## Consumer matrix (when WI-005 is in scope)

| Consumer | Owns | Calls |
|----------|------|-------|
| Foundation tests | Fixture policies + stub suite tree + stub selector | `PolicyEvaluator` / suite execute / **batch pack** |
| SBOM (optional) | User/demo DRL + suite trees + selector + Application/**Portfolio matrix** | core batch API; matrix UI in app |
| Workbench (optional) | None of the rules | Optional REST only if G-P21 locks it in |
| AR | Unlikely in this story | — |
