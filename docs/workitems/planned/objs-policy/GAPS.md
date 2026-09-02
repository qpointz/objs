# Gaps — objs-policy (C-24)

Status values: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**WI-001 must close every `open` row** (resolve or defer) before WI-002 Kotlin/Gradle work.
Draft guesses in Notes are **not** locks.

Related story: [`STORY.md`](STORY.md) · Design draft: [`docs/design/policy/overview.md`](../../../design/policy/overview.md)

---

## Open (must lock in WI-001)

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-P1 | Module split | **open** | Confirm `:objs-policy-api` / `:objs-policy-core` / `:objs-policy-drools`. Single `objs-policy` jar instead? Persistence submodule? |
| G-P2 | Package root | **open** | e.g. `org.poc.objs.policy` vs `org.poc.objs.policies` |
| G-P3 | Policy identity & versioning | **open** | UUID id? natural key `(name, version)`? immutable revisions vs overwrite? soft delete / enabled flag? |
| G-P4 | Policy body representation | **open** | Opaque `String` / `ByteArray`? charset? content-type / media type per engine? content hash algorithm? |
| G-P5 | `engineKind` enum | **open** | `DROOLS` \| `OPA` \| `CUSTOM` now; extensibility for unknown kinds (string vs sealed)? |
| G-P6 | Per-policy applicability artefact | **open** | Optional `applicabilityKind` + body on Policy vs applicability **only** via injected selector? Both? |
| G-P7 | `ApplicabilitySelector` SPI shape | **open** | Batch `select(fragment, policies) → Decision` vs per-policy `isApplicable`? Reason required? Deterministic order? |
| G-P8 | Default selector | **open** | Foundation ships `AlwaysApplicable` / `InterpretPolicyApplicabilityArtefact` / none (caller must supply)? |
| G-P9 | Enricher SPI | **open** | Chain of `FragmentEnricher`? What does enrich return (new fragment vs sidecar `EnrichmentContext` / facts bag)? |
| G-P10 | Enrich vs apply order | **open** | Always enrich-all before applicability vs enrich only for applicable (needs two-phase or applicability without enrich)? |
| G-P11 | EvaluationResult shape | **open** | Per-policy entries; **per-suite-node roll-up**; overall aggregate; engine diagnostics |
| G-P11f | Finding model | **open** | Fields: severity, message, policy ref, optional rule/id; **`entities: List<UUID>` (0..n)**; **`edges: List<UUID>` (0..n)**; ordering? require ids exist in fragment? |
| G-P12 | Aggregate status | **open** | How overall status treats mix of PASS/FAIL/ERROR/N/A (e.g. FAIL if any FAIL; ERROR dominates; N/A ignored) |
| G-P12f | Findings vs status | **open** | Can PASS carry findings (warnings)? Must FAIL have ≥1 finding? ERROR findings optional? N/A findings allowed? |
| G-P13 | PolicyRepository SPI + backing store | **open** | In-memory for tests + JPA tables in core? Flyway under objs policy module vs objs-core history? File/classpath seed import? |
| G-P14 | Repository API surface | **open** | `save` / `get` / `list` / `findByTags` / `resolve(refs)`? Pagination? Optimistic locking? Suite CRUD + membership APIs? |
| G-P15 | Orchestrator entry API | **open** | `evaluate(fragment, policyRefs)` **and** `evaluateSuite(fragment, suiteId, level?)`? Separate apply then evaluate steps public? |
| G-P16 | ERROR vs FAIL | **open** | Engine crash / bad DRL = ERROR; rule violation = FAIL. Confirm; partial results on mixed errors? |
| G-P17 | Fragment ERROR diagnostics | **open** | If `ResolvedGraphFragment` has ERROR diagnostics: refuse evaluation (like materializers) vs evaluate anyway? |
| G-P18 | Drools fact model | **open** | Generic maps from Entity/Edge vs typed fact interfaces? One KieSession per eval vs cached? Multi-policy packaging (one DRL each vs agenda groups)? |
| G-P19 | Drools version & deps | **open** | Which Drools/KIE BOM; align with Java toolchain; keep drools off api/core classpaths |
| G-P20 | Thread-safety / isolation | **open** | Stateless eval per call? Shared compiled knowledge bases keyed by policy hash? |
| G-P21 | Optional REST (`objs-policy-service`) | **open** | In this story or deferred? Paths / OpenAPI tag if in? Suite execute endpoint? |
| G-P22 | Example consumer | **open** | Defer; SBOM demo WI-005; or minimal foundation test-only fixtures only for v1 |
| G-P23 | Workbench UI | **open** | Out of story (default) vs capability-driven evaluate / suite tree later |
| G-P24 | Relation to SBOM ontology Policy | **open** | Document non-identity; any rename guidance for SBOM seeds? (likely docs-only defer) |
| G-P25 | Settings / include in `settings.gradle.kts` | **open** | When modules land (WI-002+); CI matrix impact |
| G-P26s | Suite model shape | **open** | Suite + tree of suite nodes; node fields (id, name, parent, order); cycles forbidden? |
| G-P27s | M:N membership | **open** | Membership on suite **nodes** (not only suite root); same policy on multiple nodes; membership metadata (sort order, required flag)? |
| G-P28s | Suite vs node execute scope | **open** | Execute whole suite vs selected level/subtree (like portfolio MI “level”)? |
| G-P29s | Folder roll-up formula | **open** | Parent FAIL if any child FAIL? ERROR dominates? All-N/A node → N/A or OK? Empty folder? |
| G-P30s | Dedupe when M:N | **open** | Evaluate policy once per suite run even if attached to multiple nodes; attach outcome to each placement? |
| G-P31s | SuiteRepository vs PolicyRepository | **open** | One repository facade vs split `PolicySuiteRepository` |
| G-P32s | Suite versioning | **open** | Suite/node versioning aligned with policy revisions or independent? |
| G-P33 | Suite / node applicability | **open** | Only per-policy applicability, or suite/node-level gates too (“applicable suites”)? |
| G-P34seed | Seed envelope | **open** | Same `apiVersion: objs.poc.org/v1` + `kind` multi-doc YAML as [`seeds.md`](../../../design/graph/seeds.md)? Separate policy `apiVersion`? |
| G-P35seed | Seed kinds | **open** | `Policy`, `PolicySuite` (tree inline)? Split `PolicySuiteNode` / `PolicyMembership` docs? Nested tree in one Suite doc? |
| G-P36seed | Identity / MERGE keys | **open** | Policy upsert key `(name, version)` vs UUID? Suite by `name`? Membership by (suiteNode, policyRef)? |
| G-P37seed | Body embedding | **open** | Inline DRL/Rego in YAML vs external file ref / multiline literal? Encoding for binary? |
| G-P38seed | Import path | **open** | Register `SeedDocumentHandler` in objs-policy (extends graph seed SPI) vs dedicated `PolicySeedImporter` API? Startup ledger? |
| G-P39seed | Apply order | **open** | Policies before suites/memberships; where relative to ObjectSchema/Graph if shared importer? |
| G-P40seed | Validation | **open** | Unknown engineKind? Dangling policy refs in suite? Cycle in suite tree? |
| G-P41b | Batch / result-pack API | **open** | `evaluateBatch(subjects, suiteId\|policyRefs)` shape; sync list vs streaming; max subjects; fail-fast vs continue-on-ERROR? |
| G-P42b | Pack diagnostics | **open** | Pack-level entries for refused ERROR fragments / missing policies; separate from per-subject findings? |
| G-P43b | Batch parallelism | **open** | Sequential v1 vs optional parallel subjects; shared Drools KB cache across subjects? |

---

## Provisional (likely keep — confirm in WI-001)

| # | Topic | Status | Draft resolution |
|---|--------|--------|------------------|
| G-P34 | Family naming | **resolved** (provisional) | `objs-policy*` not `objs-assessment*` |
| G-P35 | Applicability is first-class | **resolved** (provisional) | Pipeline step + SPI; N/A ≠ pass/fail |
| G-P36 | Suites are first-class | **resolved** (provisional) | Hierarchy + M:N membership + folder roll-up in foundation |
| G-P36f | Findings bind nodes/edges | **resolved** (provisional) | Each finding: `entities` 0..n + `edges` 0..n; empty OK |
| G-P36s | Seed format required | **resolved** (provisional) | Policies + suites authorable via seeds; content in apps/examples |
| G-P36b | Thin batch / result pack | **resolved** (provisional) | Opaque `subjectKey` + fragment fan-out; packed per-subject results; no matrix |
| G-P37 | First engine | **resolved** (provisional) | Drools in `:objs-policy-drools` |
| G-P38 | No product rules in foundation | **resolved** (provisional) | Examples/apps own concrete policies, suite trees & product selectors |
| G-P39 | Not default on `:objs-service` | **resolved** (provisional) | Opt-in like jgrapht-service |
| G-P40 | Input is GraphFragment | **resolved** (provisional) | Same resolve path as Gremlin/JGraphT |
| G-P41 | Policy ≠ graph entity | **resolved** (provisional) | Dedicated PolicyRepository / suite store |
| G-P42 | Suite ≠ SBOM Portfolio | **resolved** (provisional) | Shape analogy only; separate store |
| G-P43 | Matrix is product | **resolved** (provisional) | Portfolio × suite (or any multi-axis) assessment grid stays in apps |

---

## Out of story / deferred

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | OPA / Rego adapter | **deferred** | Follow-up module `:objs-policy-opa` |
| G-X2 | Compliance scoring product | **deferred** | Product/UX |
| G-X3 | Fingerprint approval workflows | **deferred** | SBOM product |
| G-X4 | Replace persist-gate validation | **cancelled** | Orthogonal ([`validation.md`](../../../design/graph/validation.md)) |
| G-X5 | Elementary “policy authoring UI” / suite tree UI | **deferred** | Workbench / example |
| G-X6 | Distributed policy sync / multi-tenant ACL | **deferred** | Ops concern |
| G-X7 | Reuse SBOM portfolio tables for suites | **cancelled** | Suites are foundation artefacts; portfolios stay domain-only |
| G-X8 | Portfolio × suite assessment matrix UI/report | **deferred** | Product (e.g. SBOM MI-style); uses foundation batch pack only |

---

## Decision log (fill during WI-001)

| # | Decision | Date | Summary |
|---|----------|------|---------|
| — | — | — | — |
