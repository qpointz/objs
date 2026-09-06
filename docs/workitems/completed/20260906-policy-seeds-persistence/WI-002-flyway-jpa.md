# WI-002 — Flyway + JPA catalog repositories

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** WI-001  

## Goal

Persist **Policy**, **Category**, and **Suite** (folders/matchers) on the **objs Flyway** line in **`:objs-persistence`** (G-P13p). Align model/API human identity to **`key`** (G-P36seed). Same repository **ports** (G-P14p).

## Scope

- [x] Flyway migrations under `org/poc/objs/core/db/migration/{vendor}/` (next objs `VN`, both `postgresql` and `h2`)
- [x] JPA / DAO in `:objs-persistence` for `PolicyRepository`, `CategoryRepository`, `SuiteRepository`
- [x] Model/API: `key` identity (replace category `slug`, policy/suite name-as-identity); keep display `name`; `PolicyRef` / find-by-key align
- [x] Wire Boot / play path to durable store where appropriate (`ObjsPolicyPersistenceAutoConfiguration`)
- [x] Unit / IT coverage for CRUD + resolve/query
- [x] Workbench UI field rename (`key` / `name`)

## Out of scope

- Seed import / Drop handlers (WI-003)
- Catalog REPLACE export / workbench (WI-005)
- Evaluation result / input persist tables
- Changing flat evaluate / evaluateSuite semantics

## Acceptance

- [x] Catalog survives process restart (IT or equivalent)
- [x] Ports preserve behaviour with `key` identity (G-P14p + G-P36seed)
