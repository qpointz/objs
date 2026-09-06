# WI-003 — Seed handlers + import tests

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-002  

## Goal

Policy catalog **`SeedDocumentHandler`**s in objs-policy\* (G-P38seed): independent kinds, apply/replace, Drop*, user file/document order, validation + fail-file/no-ledger (G-P35seed…G-P40seed). Export = **WI-005**.

## Scope

- [x] Kinds: `Category`, `Policy`, `PolicySuite` + `DropCategory`, `DropPolicy`, `DropPolicySuite` (G-P35seed) — `PolicySeedKinds` (`:objs-policy-api`); handlers in `org.poc.objs.policy.service.seed` (`:objs-policy-service`)
- [x] apply/MERGE vs replace (symmetric for all three content kinds; category/suite cascades per GAPS)
- [x] MERGE / resolve on `key` (G-P36seed)
- [x] Body: inline + file ref + classpath load (G-P37seed) — `body` / `bodyRef` / `bodyFile`, `SpringSeedBodyLoader` (`ClassPathResource` / `FileSystemResource`)
- [x] Handlers only — no dedicated importer (G-P38seed); registered as `@Bean`s in `ObjsPolicyServiceAutoConfiguration`, collected by the shared `:objs-autoconfigure` `SeedImporter`
- [x] No forced kind apply-order; file + document order only (G-P39seed) — all handlers share `applyOrder = 50`
- [x] Validation (G-P40seed): dangling refs (categoryKey / policyKey), suite cycles (via `SuiteTreeValidator`), `key` rules (`PolicyKeys.requireValid`); **fail whole file**; **no ledger update** on failure — `SeedStartupLoader` no longer calls `ledger.recordFailure` on import failure
- [x] Import tests (apply vs replace; Drop*; dangling refs; bodyRef classpath) — `PolicySeedImportTest` (`:objs-policy-service`, 14 tests) using `InMemoryPolicyStores` + `PassthroughUnitOfWork` (no Spring)

## Out of scope

- Full-catalog REPLACE **export** + workbench (WI-005)
- Product/regulatory seed **content** in foundation
- Evaluation-result tables (G-P11r)
- Batch (C-29)

## Acceptance

- [x] Independent docs import in user file/document order
- [x] apply/replace + Drop* match G-P35seed
- [x] Failed resource does not advance seed ledger (G-P40seed) — updated `SeedStartupLoaderIT` (`:objs-persistence`) to assert the ledger is untouched on failure
