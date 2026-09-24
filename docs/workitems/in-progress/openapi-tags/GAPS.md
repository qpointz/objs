# Gaps — openapi-tags (C-42)

Design lock (WI-001) must close open items before apply / completeness / codegen WIs.

## Scope (locked)

| Id | Decision | When |
|----|----------|------|
| G-0 | **In scope:** all `/api/v1/objs/**` controllers + SBOM domain + AR domain. Same objs tags/docs wherever that prefix is mounted. | 2026-09-24 |
| G-0b | Story includes **completeness** of OpenAPI (Swagger as comprehensive source) **and** **Java + Python** client **codegen smoke** (manual localhost OK). | 2026-09-24 |

## Open

_(none — design GAPS closed; exact tag name matrix still written in WI-001)_

## Closed / deferred

| Id | Decision | When |
|----|----------|------|
| G-0 | See Scope (locked) | 2026-09-24 |
| G-0b | See Scope (locked) | 2026-09-24 |
| G-1 | Split fat graph ops by **topic** (method-level `@Tag`). Draft: **graphs** (header CRUD/list/search), **mutations** (mutate + validate), **entities** (attach/detach; may share pool tag), **edges** (graph-scoped CRUD; may share pool tag), **versions**, **housekeeping** (clear/destroy/clone/purge), **query** if kept separate. Exact names in WI-001 matrix. | 2026-09-24 |
| G-2 | **Policy** tags: topic split without `policy-` prefix: **catalog**, **evaluate**, **suites**, **archives**. One springdoc **`policy`** group (G-3). | 2026-09-24 |
| G-3 | **springdoc groups (objs):** (1) **`policy`** = separate OpenAPI definition/group for `/api/v1/objs/policy/**` (policy service surface — not mixed into `graph`/`registry`). Prefer bean living with `:objs-policy-service` when practical. (2) **`graph`** group includes pool **entities** + pool **edges** together (`/api/v1/objs/entities/**`, `/api/v1/objs/edges/**`, plus existing graphs/graph/status paths as today). (3) **`registry`** stays its own group. No separate `edges` group. | 2026-09-24 |
| G-3b | **`traverse`** and **`algorithms`**: fold paths into the **`graph`** OpenAPI group; **no** separate springdoc groups. Separate topic tags. | 2026-09-24 |
| G-4 | Seed I/O tag **`seeds`** (not `graph` / `graph-seeds`). Keep **`graphs`** for named-graph headers. Springdoc **group** id stays `graph`. | 2026-09-24 |
| G-4b | **Registry** topic tags (same style): **catalog**, **schemas**, **edges** inside group **`registry`**. | 2026-09-24 |
| G-5 | **SBOM:** mirror G-1 — topic tags on fat surfaces. Split mega-`inventory` (apps controller): draft **applications**, **application-versions**, **application-sboms** (and keep existing sibling tags `assets` / `portfolios` / `schemas` / `assessment`). Exact names in WI-001. Springdoc group **`inventory`** stays (or rename only if WI-001 says so). | 2026-09-24 |
| G-6 | **AR:** mirror G-1 — topic tags on `AssetRepositoryController`, not one mega-`asset-repository` tag. Draft: **collections**, **objects** (incl. search/compositions/relations), **schemas** (catalog/schema browse). Exact names in WI-001. Springdoc group **`asset-repository`** stays. | 2026-09-24 |
| G-7 | **Completeness bar** (Swagger as primary docs) for every in-scope public operation: (1) operation **summary** + **description** when non-obvious; (2) **input parameter** descriptions (path/query/header); (3) request/response **DTO** `@Schema` description and **field** descriptions; (4) documented **HTTP status codes** with response schemas for success and important errors. Apply to **`/api/v1/objs/**`** (main gap) **and** re-audit SBOM/AR to the same bar. | 2026-09-24 |
| G-8 | **Codegen targets:** full Java/Python generate **recipes** for workbench localhost specs **`graph`** (objs graph definition), **`registry`**, and **`policy`** (`/v3/api-docs/{group}`). **SBOM** (`inventory`) and **AR** (`asset-repository`): **readiness check only** (confirm spec is codegen-friendly / one-shot generate OK) — no required maintained recipe unless trivial to share. | 2026-09-24 |
| G-9 | **Tooling:** **openapi-generator-cli** (pin version in docs/script). Generators: **Java** + **Python**. Document how to generate (operator-facing). Default fetch base **`http://localhost:8080`** (overrideable). Deliver a **manual harness script** (e.g. under `scripts/` or `docs/…`) that downloads specs, generates into a **gitignored/temp** dir, and verifies health — **do not** commit generated sources into the repo. | 2026-09-24 |
| G-10 | **Success bar:** generated Java **compiles** and generated Python **imports/compiles** (bytecode/`compileall` or equivalent). Not a full client integration suite. Harness = manual good-shape check for G-8 primary groups; SBOM/AR readiness may reuse the same harness in a lighter mode. | 2026-09-24 |
