# Policy evaluation — detailed design

**Status:** C-24 S1 **shipped** (`:objs-policy-api` / `:objs-policy-core`); later stories still open — see README  
**Normative locks:** [`policy-evaluate-core/GAPS.md`](../../workitems/in-progress/policy-evaluate-core/GAPS.md) (S1); other stories own their GAPS  
**Audience:** foundation embedders, design-lock reviewers, example-app authors  
**Folder index:** [`README.md`](README.md)

Shared philosophy for foundation policy evaluation. **S1 flat evaluate is implemented.** Drools, workbench, suites, seeds, batch, and consumers remain separate stories.

### S1 design pages (normative detail + diagrams)

| Page | Topic |
|------|--------|
| [`model.md`](model.md) | Identity, serial version, body, engineKind, applicability fields |
| [`pipeline.md`](pipeline.md) | Resolve → PolicyContextWiring → gated evaluate |
| [`evaluation-sequences.md`](evaluation-sequences.md) | Sequence diagrams for implement + documenting pass |
| [`modules.md`](modules.md) | `:objs-policy-api` / `:objs-policy-core` (shipped types) |
| [`results.md`](results.md) | Outcomes, findings, ERROR vs FAIL, aggregate helper |
| [`repository.md`](repository.md) | In-memory repo; resolve latest\|version\|id |

Also: [`GAPS.md`](../../workitems/in-progress/policy-evaluate-core/GAPS.md) · [`EXAMPLES.md`](../../workitems/in-progress/policy-evaluate-core/EXAMPLES.md)

---

## Locked (S1 / C-24) — shipped

| Topic | Lock |
|-------|------|
| Modules | `:objs-policy-api` + `:objs-policy-core`; `org.poc.objs.policy.{api,core}` — [`modules.md`](modules.md) |
| Policy | name + serial version (new on create/update); no `enabled`; body String + optional contentType; `engineKind` String (`CUSTOM` only in S1) — [`model.md`](model.md) |
| Applicability | optional kind+body on Policy; S1 ALWAYS_APPLY / blank; **bound into** `evaluate` (optional `applicability` preview); cannot skip gate — [`pipeline.md`](pipeline.md) |
| Context / wiring | `PolicyEvaluationContext` + **`PolicyContextWirer`** (PolicyContextWiring) — first after resolve; not Enricher — [`pipeline.md`](pipeline.md) |
| Pipeline | resolve → PolicyContextWiring → gated evaluate; refuse fragment ERROR diagnostics — [`pipeline.md`](pipeline.md) |
| Results | per-policy outcomes authoritative (+ version); optional flat aggregate helper; findings optional + soft validation — [`results.md`](results.md) |
| ERROR vs FAIL | FAIL = not satisfied; ERROR = engine/body/unknown-kind; continue others — [`results.md`](results.md) |
| Repo | in-memory; save→new version; resolve latest\|version\|id — [`repository.md`](repository.md) |
| Entry | fixed `evaluate(fragment, policyRefs)`; wrappers later — [`pipeline.md`](pipeline.md) |
| Boundaries | no product rules in foundation; not default on `:objs-service`; Policy ≠ graph entity; input = GraphFragment |

**Shipped entry points:** `DefaultPolicyEvaluator`, `InMemoryPolicyRepository`, `AlwaysApplyApplicabilitySelector`, `CustomPolicyEngine` (`org.poc.objs.policy.core`).

**Deferred to later stories:** Drools (C-26), workbench (C-31), suites (C-27), seeds/JPA (C-28), batch (C-29), example/REST consumer (C-30).

Sections **§1+** below remain illustrative for the **full-family** vision (suites, seeds, batch, Drools). Where they conflict with the table above or the S1 pages, **S1 locks win**.

---

## 1. Problem and intent

Graphs in objs already represent applications and their composition (e.g. SBOM Combined BOM). Operators need to **assess** those graphs against **user-defined** rules: version gates, banned technologies, registration requirements, governance packs, and so on.

The foundation must not embed those rules. It must provide:

1. A place to **store** executable policy artefacts and **suite** taxonomies  
2. A **pipeline** that takes any graph fragment, selects what applies, evaluates with pluggable engines, and returns a **unified** result  
3. **Findings** that can point at **0..n nodes** and **0..n edges**  
4. A **seed format** so policy packs are authored like ontology/graph seeds  
5. A **thin batch / result pack** so products can fan out many subjects without foundation owning matrices  

“Assessment” is an optional product word for a run or result. The module family is **`objs-policy`**.

```mermaid
flowchart LR
  subgraph input [Input]
    Frag[GraphFragment]
    Pack[Policy pack / suite]
    BatchIn[Optional subject batch]
  end
  subgraph foundation [objs-policy]
    Repo[Repository]
    Pipe[PolicyContextWiring · gated evaluate]
    Engines[PolicyEngine adapters]
    Batch[Thin result pack]
  end
  subgraph output [Output]
    Res[EvaluationResult]
    Find[Findings with node/edge bindings]
    Roll[Suite folder roll-up]
    PackOut[BatchEvaluationResult]
  end
  Frag --> Pipe
  Pack --> Repo
  Repo --> Pipe
  Pipe --> Engines
  Engines --> Res
  Res --> Find
  Res --> Roll
  BatchIn --> Batch
  Batch --> Pipe
  Batch --> PackOut
```

---

## 2. Naming and non-conflation

| Term | Means | Does **not** mean |
|------|--------|-------------------|
| **Policy** (foundation) | Executable artefact: metadata + `engineKind` + body (+ optional applicability) | SBOM ontology entity type `Policy` / `COMPLIES_WITH` |
| **PolicySuite** | Hierarchical taxonomy of folders referencing policies | SBOM **Portfolio** / subject-area tables |
| **GraphFragmentPolicy** | Fragment **normalization** (identity, duplicates, dangling ends) | Business / governance rules |
| **Persist validation** | JSON Schema + allow-list at write | Compliance scoring |
| **Assessment** | Product/UX label for a run or result | Module or package name |

```mermaid
flowchart TB
  subgraph doNotConflate [Do not conflate]
    GFP[GraphFragmentPolicy normalize]
    VAL[Persist validation schema]
    POL[objs-policy executable rules]
    SBOM[SBOM Policy entity metadata]
    PORT[SBOM Portfolio apps taxonomy]
  end
  GFP -.->|not| POL
  VAL -.->|not| POL
  SBOM -.->|not| POL
  PORT -.->|shape analogy only| POL
```

---

## 3. Foundation vs product boundary

```mermaid
flowchart TB
  subgraph foundation [Foundation objs-policy]
    Model[Policy + Suite model]
    Store[Policy / suite repository]
    SeedsFmt[Seed format + importer]
    SPI[PolicyContextWirer + ApplicabilitySelector + PolicyEngine SPI]
    Orch[Orchestration + roll-up]
    Result[Unified result DTOs]
    Drools[objs-policy-drools adapter]
  end
  subgraph product [Examples / apps]
    Content[Concrete DRL / Rego / packs]
    Tree[IT Governance suite tree content]
    Select[Product applicability predicates]
    Bind[Application / Portfolio / fingerprint binding]
    Matrix[Portfolio x suite assessment matrix]
    UX[Scoring UX approval workflows]
  end
  product -->|calls| foundation
  Content -->|loaded via seeds| Store
  Tree -->|loaded via seeds| Store
  Select -->|implements| SPI
  Matrix -->|uses batch pack only| Orch
```

| Foundation owns | Product / examples own |
|-----------------|-------------------------|
| Data model, repository, seeds **format** | Seed **content** (PCI, Mongo gates, “IT Governance” tree) |
| Applicability **SPI** | “Has Database” / Apigee predicates |
| Drools **adapter** | Production DRL text |
| Folder **roll-up algorithm** (within one suite run) | When to run which suite against which app |
| **Thin batch / result pack** (opaque subjectKey) | **Assessment matrix** layout and axes |

---

## 4. Module map

**S1 (locked):** only `:objs-policy-api` + `:objs-policy-core` — see [`modules.md`](modules.md).

```mermaid
flowchart TB
  api[objs-api GraphFragment]
  papi[objs-policy-api]
  pcore[objs-policy-core]
  pdrools[objs-policy-drools C-26]
  popa[objs-policy-opa later]
  psvc[objs-policy-service optional]
  app[Example apps / workbench runner]

  api --> papi
  papi --> pcore
  papi --> pdrools
  papi --> popa
  pcore --> psvc
  pdrools --> psvc
  pcore --> app
  pdrools --> app
  psvc -.->|opt-in not default on objs-service| app
```

| Module | Responsibility | When |
|--------|----------------|------|
| `:objs-policy-api` | Spring-free contracts: Policy, SPIs, findings, results (+ Suite types later) | **C-24** |
| `:objs-policy-core` | In-memory repo, wire → gated evaluate; later seeds/suites as stories land | **C-24** (+ C-27/C-28) |
| `:objs-policy-drools` | First real `PolicyEngine` | **C-26** |
| `:objs-policy-opa` | Later | deferred |
| `:objs-policy-service` | Optional REST (jgrapht-service pattern); **not** on `:objs-service` by default | C-30 / C-31 as needed |

---

## 5. Conceptual data model

### 5.1 Entities (logical ER)

```mermaid
erDiagram
  POLICY ||--o{ POLICY_MEMBERSHIP : "placed in"
  SUITE_NODE ||--o{ POLICY_MEMBERSHIP : "contains"
  SUITE ||--|{ SUITE_NODE : "has tree"
  SUITE_NODE ||--o{ SUITE_NODE : "parent child"
  POLICY {
    uuid id
    string name
    long serialVersion
    string engineKind
    string evaluationBody
    string contentType
    string applicabilityKind
    string applicabilityBody
  }
  SUITE {
    uuid id
    string name
    string version
  }
  SUITE_NODE {
    uuid id
    string key
    string name
    int sortOrder
  }
  POLICY_MEMBERSHIP {
    uuid policyId
    uuid suiteNodeId
    int sortOrder
  }
```

**Many-to-many:** one policy may appear under many suite nodes (across one or many suites). One suite node may list many policies.

**Hierarchy:** suite nodes form a tree (cycles forbidden — validation gap `G-P40seed`). Analogy: SBOM portfolio → subject areas; here suite → folders → **policy refs**, not applications.

### 5.2 Policy artefact

**S1 normative fields:** [`model.md`](model.md) (name + serial version; String body; no `enabled`).

| Field | Role | Notes |
|-------|------|--------|
| Identity | `name` + serial `version` (+ row `id`) | S1 locked; seed MERGE keys = C-28 |
| `engineKind` | String: `CUSTOM` (S1), later `DROOLS` / … | Not an enum |
| Evaluation body | Opaque UTF-8 String | + optional `contentType` |
| Optional applicability | `applicabilityKind` + `applicabilityBody` | S1: blank / `ALWAYS_APPLY` |
| Metadata | description, tags, timestamps | Optional; **no** policy-level enabled |

Policies are **not** rows in `bom_entity`. Dedicated policy store (in-memory S1; JPA C-28).

### 5.3 Suite tree (illustrative product content)

```mermaid
flowchart TB
  root["IT Governance"]
  db["Database"]
  api["API"]
  p1["Policy: Mongo up to date"]
  p2["Policy: do not use FoxPro"]
  p3["Policy: registered in Apigee"]
  root --> db
  root --> api
  db --> p1
  db --> p2
  api --> p3
```

After a suite run (example outcomes):

| Node / policy | Status |
|---------------|--------|
| Mongo up to date | PASS |
| do not use FoxPro | PASS |
| Database | **OK** |
| registered in Apigee | FAIL |
| API | **FAIL** |
| IT Governance | **FAIL** |

---

## 6. Graph input

Any producer of `GraphFragment` is valid input:

- Full named graph select  
- Matcher selection / Explore-scope fragment  
- Multi-graph union (e.g. SBOM Combined)  
- Builder / codegen fragment  

Always normalize first with existing **`GraphFragmentPolicy.resolve`** ([`fragments-and-analysis.md`](../graph/fragments-and-analysis.md)):

```mermaid
flowchart LR
  Raw[GraphFragment]
  Resolve[GraphFragmentPolicy.resolve]
  Resolved[ResolvedGraphFragment]
  Raw --> Resolve --> Resolved
```

**S1 lock:** if resolved fragment carries **ERROR** diagnostics, refuse evaluation (`PolicyEvaluationException`) — same spirit as Gremlin/JGraphT materializers.

---

## 7. Evaluation pipelines

### 7.1 Flat policy collection (S1)

**Normative diagrams and contract:** [`pipeline.md`](pipeline.md).

Applicability is **bound into** `evaluate` (not a skippable outer stage). Optional `applicability(...)` preview shares the same gate.

### 7.2 Suite execution

```mermaid
sequenceDiagram
  participant Caller
  participant Orch as PolicyEvaluator
  participant SuiteRepo as SuiteRepository
  participant Appl as ApplicabilitySelector
  participant Eng as PolicyEngine
  participant Roll as RollUp
  Caller->>Orch: evaluateSuite(fragment, suiteId, level?)
  Orch->>SuiteRepo: load suite tree + memberships
  SuiteRepo-->>Orch: nodes + policy refs
  Orch->>Orch: dedupe policies M:N
  Orch->>Orch: resolve + wiring + applicability + evaluate
  Orch->>Roll: aggregate per node and root
  Roll-->>Orch: node statuses
  Orch-->>Caller: SuiteEvaluationResult
```

**Collect → evaluate → roll up.** Flat evaluate remains supported without a suite.

**Execute scope (`G-P28s`):** whole suite vs selected level/subtree (portfolio MI “level” analogy) — open.

**Dedupe (`G-P30s`):** if the same policy is attached to two nodes, evaluate **once** per run (draft); attach outcome to each placement for display — open.

---

## 8. Applicability (first-class)

Applicability answers: *is this policy in scope for this fragment?* It is **not** pass/fail.

**S1:** blank / `ALWAYS_APPLY` only; gate always runs inside `evaluate` — [`pipeline.md`](pipeline.md) · [`model.md`](model.md).

**Example (later kinds):** “PostgreSQL must be version > 16.5”

| Fragment | Applicability | Evaluation |
|----------|---------------|------------|
| Has Postgres 16.4 | yes | FAIL + finding on component entity |
| Libraries only, no DB | no | **NOT_APPLICABLE** (not pass, not fail) |

```mermaid
flowchart LR
  Pol[Policy body version check]
  Gate[Applicability has Database?]
  Frag[Fragment]
  Frag --> Gate
  Gate -->|yes| Pol
  Gate -->|no| NA[NOT_APPLICABLE]
```

**Family requirements:**

- Gate bound to evaluate + optional preview (S1 locked)  
- Optional per-policy applicability artefact (S1 fields present; kinds beyond ALWAYS_APPLY later)  
- Must **not** rely only on Drools `when` for N/A visibility across engines  
- Suite/node-level applicability = C-27

---

## 9. Engines

```mermaid
flowchart TB
  Orch[Orchestrator]
  SPI[PolicyEngine SPI]
  D[Drools adapter]
  O[OPA later]
  C[CUSTOM / stub]
  Orch --> SPI
  SPI --> D
  SPI --> O
  SPI --> C
```

| Kind | Module | Role |
|------|--------|------|
| `DROOLS` | `:objs-policy-drools` | First real adapter; facts from wired context / fragment; body = DRL (or locked format) |
| `OPA` | later | Deferred |
| `CUSTOM` | tests / apps | Stub or app-specific |

Engine receives **already-applicable** policies only. Drools fact mapping / session lifecycle: C-26 GAPS.

Foundation must not ship regulatory DRL in `src/main` — fixtures and example packs only.

---

## 10. Findings and evidence bindings

**S1 normative:** [`results.md`](results.md) — findings optional on all statuses; FAIL may have zero; soft validation only.

```mermaid
erDiagram
  POLICY_OUTCOME ||--o{ FINDING : "has"
  FINDING {
    string severity
    string message
    string ruleId
  }
  FINDING ||--o{ ENTITY_REF : "entities 0..n"
  FINDING ||--o{ EDGE_REF : "edges 0..n"
  ENTITY_REF {
    uuid entityId
  }
  EDGE_REF {
    uuid edgeId
  }
```

```mermaid
flowchart LR
  F1[Finding version too low]
  E1[Entity Postgres component]
  F2[Finding forbidden USES]
  E2[Entity FoxPro]
  Ed1[Edge USES]
  F3[Finding org policy note]
  F1 --> E1
  F2 --> E2
  F2 --> Ed1
  F3 -.->|empty bindings| None[No graph locus]
```

Same spirit as `GraphFragmentDiagnostic` node/edge lists, but for **evaluation**, not fragment normalization.

---

## 11. Status and suite roll-up

### 11.1 Per-policy status (S1 locked)

| Status | Meaning |
|--------|---------|
| `PASS` | Applicable and satisfied |
| `FAIL` | Applicable and violated |
| `ERROR` | Engine/policy execution failure (bad body, crash, unknown kind) |
| `NOT_APPLICABLE` | Out of scope for this fragment |

**Flat aggregate helper (S1):** optional convenience only — `ERROR > FAIL > PASS > N/A` — see [`results.md`](results.md). **Not** suite roll-up.

**Suite folder roll-up (C-27):**

- `ERROR` dominates when present  
- Else `FAIL` if any FAIL among considered children  
- `NOT_APPLICABLE` does **not** count as failure  
- All-N/A / empty folder behavior = suite GAPS  

```mermaid
flowchart TB
  Children[Child node statuses + member policy outcomes]
  Agg{Aggregate}
  Children --> Agg
  Agg -->|any ERROR| ER[ERROR]
  Agg -->|else any FAIL| FL[FAIL]
  Agg -->|else any PASS and no FAIL| OK[OK / PASS]
  Agg -->|only N/A or empty| NA[N/A or OK TBD]
```

### 11.2 Roll-up on the IT Governance example

```mermaid
flowchart TB
  root["IT Governance FAIL"]
  db["Database OK"]
  api["API FAIL"]
  p1["Mongo PASS"]
  p2["FoxPro PASS"]
  p3["Apigee FAIL"]
  p1 --> db
  p2 --> db
  p3 --> api
  db --> root
  api --> root
```

---

## 12. Seeds

Policies and suites **must** have a portable seed format (graph-seeds family).

**Draft envelope** (align with [`seeds.md`](../graph/seeds.md) unless `G-P34seed` chooses otherwise):

```yaml
apiVersion: objs.poc.org/v1
kind: Policy
name: mongo-up-to-date
version: "1"
engineKind: DROOLS
body: |
  # product DRL — not foundation content
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

```mermaid
sequenceDiagram
  participant App as Example app / CLI
  participant Imp as Policy seed importer
  participant Repo as PolicyRepository
  App->>Imp: multi-doc YAML
  Imp->>Imp: validate all docs
  Imp->>Repo: MERGE Policy docs
  Imp->>Repo: MERGE PolicySuite + memberships
  Note over Imp,Repo: Policies before suite memberships G-P39seed
```

| Concern | Gap |
|---------|-----|
| Envelope / kinds / nesting | `G-P34seed`, `G-P35seed` |
| MERGE identity keys | `G-P36seed` |
| Inline body vs file ref | `G-P37seed` |
| `SeedDocumentHandler` vs dedicated importer | `G-P38seed` |
| Apply order / validation | `G-P39seed`, `G-P40seed` |

**Content** (real governance packs) lives under examples/apps. Foundation owns format + importer only.

---

## 13. PolicyContextWiring

**S1 lock:** `PolicyContextWirer`s only **wire into** `PolicyEvaluationContext` (sidecar bag); run **first** after resolve so applicability can use context — [`pipeline.md`](pipeline.md).

```mermaid
flowchart LR
  R[ResolvedGraphFragment]
  Wirer[PolicyContextWirer chain]
  Ctx[PolicyEvaluationContext]
  R --> Wirer --> Ctx
```

No built-in wirers; no topology rewrite in foundation.

---

## 14. Repository and orchestration API

**S1:** [`repository.md`](repository.md) · [`pipeline.md`](pipeline.md) — `evaluate(fragment, policyRefs)` + optional `applicability(...)`; in-memory `PolicyRepository`.

```mermaid
classDiagram
  class PolicyRepository {
    save(policy)
    resolve(ref)
    list(...)
  }
  class PolicySuiteRepository {
    save(suite)
    get(id)
    getTree(suiteId)
  }
  class PolicyEvaluator {
    evaluate(fragment, policyRefs)
    applicability(fragment, policyRefs)
    evaluateSuite(fragment, suiteId, level)
    evaluateBatch(subjects, target)
  }
  class BatchEvaluationResult {
    items List~SubjectResult~
    packDiagnostics
  }
  class SubjectResult {
    subjectKey String
    result EvaluationResultOrSuite
  }
  class PolicyEngine {
    evaluate(context, policy) PolicyOutcome
  }
  PolicyEvaluator --> PolicyRepository
  PolicyEvaluator --> PolicySuiteRepository
  PolicyEvaluator --> PolicyEngine
  PolicyEvaluator --> BatchEvaluationResult
```

Suite / batch methods = later stories (wrappers over the fixed S1 contract).

---

## 15. Thin batch / result pack (foundation) vs matrix (product)

**Provisional lock:** foundation includes a **thin** batch facility so products can build multi-dimensional assessment views. The **matrix itself** (portfolio × suite, heatmaps, cross-subject scores) stays in the app.

```mermaid
flowchart TB
  subgraph product [Product e.g. SBOM]
    Port[Portfolio level to app set]
    Frags[Build fragment per app]
    Matrix[Assessment matrix UI]
  end
  subgraph foundation [objs-policy]
    Batch[evaluateBatch opaque subjectKeys]
    Pack[BatchEvaluationResult packed cells]
  end
  Port --> Frags
  Frags --> Batch
  Batch --> Pack
  Pack --> Matrix
```

```mermaid
sequenceDiagram
  participant Sbom as SBOM app
  participant Pol as PolicyEvaluator
  Sbom->>Sbom: portfolio level to apps A B C
  Sbom->>Pol: evaluateBatch([{A,fragA},{B,fragB},{C,fragC}], suite)
  Pol-->>Sbom: pack keyed by subjectKey
  Sbom->>Sbom: render matrix apps x suite folders
```

| In foundation | Out of foundation |
|---------------|-------------------|
| Many `{ subjectKey, fragment }` × one suite or policy collection | Named axes (portfolio, MI report) |
| Packed ordered list of per-subject results | Grid layout / joins of two taxonomies |
| Optional pack-level diagnostics only | Cross-subject roll-up or portfolio score |
| Opaque `subjectKey` (caller interprets) | Assuming subjects are Applications |

Single-fragment evaluate/suite remains the core; batch is a convenience on the same orchestrator.

---

## 16. Motivating end-to-end (SBOM-shaped)

```mermaid
sequenceDiagram
  participant Op as Operator
  participant Sbom as SBOM app
  participant Store as GraphStore
  participant Pol as objs-policy
  Op->>Sbom: Assess app version with suite IT Governance
  Sbom->>Store: select Combined SBOM fragment
  Store-->>Sbom: GraphContents
  Sbom->>Pol: evaluateSuite(fragment, it-governance)
  Note over Sbom,Pol: App supplies ApplicabilitySelector
  Pol-->>Sbom: SuiteEvaluationResult + findings
  Sbom-->>Op: Tree statuses + highlight nodes/edges
```

For portfolio-level management views, the app loops subjects (or calls **batch**) and owns the matrix — see **§15** and follow-up [`policy-batch`](../../workitems/planned/policy-batch/STORY.md) (C-29).

SBOM Application / Portfolio binding stays in the app. Foundation never requires SBOM types.

---

## 17. Non-goals

- OPA adapter in the first implementation story (follow-up)  
- Hard-coded regulatory catalogs or “IT Governance” content in foundation jars  
- Treating `NOT_APPLICABLE` as PASS or FAIL  
- Replacing persist-gate validation  
- Storing suites in SBOM portfolio tables  
- Default dependency from `:objs-service` on policy modules  
- Full compliance scoring / approval product UX  
- **Portfolio × suite assessment matrix** (or any multi-axis grid) inside foundation — product only; use thin batch pack  

---

## 18. Open decisions (story-scoped)

| Story | Gaps |
|-------|------|
| C-24 S1 flat evaluate | [`policy-evaluate-core/GAPS.md`](../../workitems/in-progress/policy-evaluate-core/GAPS.md) |
| C-26 Drools | [`policy-drools/GAPS.md`](../../workitems/planned/policy-drools/GAPS.md) |
| C-31 Workbench Policy play | [`policy-workbench/GAPS.md`](../../workitems/planned/policy-workbench/GAPS.md) |
| C-27 Suites | [`policy-suites/GAPS.md`](../../workitems/planned/policy-suites/GAPS.md) |
| C-28 Seeds + persistence | [`policy-seeds-persistence/GAPS.md`](../../workitems/planned/policy-seeds-persistence/GAPS.md) |
| C-29 Batch | [`policy-batch/GAPS.md`](../../workitems/planned/policy-batch/GAPS.md) |
| C-30 Example/REST consumer | [`policy-consumer/GAPS.md`](../../workitems/planned/policy-consumer/GAPS.md) |

**C-24 WI-001 is closed.** Implement api/core against S1 locks (WI-002+). Do not pull later-story gaps into C-24.

---

## 19. Related documents

| Doc | Why |
|-----|-----|
| [`README.md`](README.md) | Folder index |
| [`model.md`](model.md) · [`pipeline.md`](pipeline.md) · [`evaluation-sequences.md`](evaluation-sequences.md) · [`modules.md`](modules.md) · [`results.md`](results.md) · [`repository.md`](repository.md) | S1 normative design pages |
| [`STORY.md`](../../workitems/in-progress/policy-evaluate-core/STORY.md) | C-24 delivery plan (flat MVP) |
| [`EXAMPLES.md`](../../workitems/in-progress/policy-evaluate-core/EXAMPLES.md) | S1 scenarios; follow-ups linked |
| [`SEQUENCE.md`](../../workitems/SEQUENCE.md) | C-24…C-31 order |
| [`fragments-and-analysis.md`](../graph/fragments-and-analysis.md) | Fragment resolve contract |
| [`seeds.md`](../graph/seeds.md) | Graph seed envelope to align with |
| [`apps-vs-foundation.md`](../graph/apps-vs-foundation.md) | Foundation vs examples |
| [`validation.md`](../graph/validation.md) | Persist gate — orthogonal |
| [`sbom/example.md`](../sbom/example.md) | Portfolio shape analogy only |
