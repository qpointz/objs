# Gaps & clarifications — entity-rest-api

Open decisions for [`STORY.md`](STORY.md). Resolve here or in design docs before / during the listed WIs.
Do not invent silently in code without updating this file or [`docs/design/service/`](../../../design/service/).

**Legend:** `blocking` = prefer decide before that WI · `default-ok` = sensible default if unset · `half-open` = intentional defer

---

## Graph API

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-R1 | **GET `/graph` annotation filter encoding** | default-ok | See detail below |
| G-R2 | **Empty / missing annotation filter on GET `/graph`** | default-ok | **Reject `400`** — never load-all over HTTP |
| G-R3 | **DELETE `/graph` body shape** | default-ok | See detail below |
| G-R4 | **DELETE batch semantics** | default-ok | **All-or-nothing** in one transaction; any unknown id → fail whole request |
| G-R5 | **PUT `/graph` response** | default-ok | Return the **same graph** after id assignment (entities + edges with ids filled) |
| G-R6 | **POST `/graph/validate` response** | default-ok | HTTP `200` + `BoMValidationResult` JSON even when invalid (`isValid: false`); do not persist. Optionally use `400` when invalid — **prefer always 200 with body** so clients can inspect issues without treating transport as failure |

## Registry API

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-R7 | **Schema PUT replace vs create-only** | default-ok | **Upsert** — replace document if `(type, version)` already exists |
| G-R8 | **DELETE missing schema / edge definition** | default-ok | **`404`** |
| G-R9 | **Edge DELETE identity (`/registry/edges`)** | default-ok | Query params `sourceType`, `role`, `targetType` (exact triple key, including `*`) |
| G-R10 | **Path `type` / `version` encoding** | default-ok | Path segments URL-decoded; types/versions should avoid `/`; no further encoding scheme in this story |
| G-R11 | **Validate JSON Schema document on register** | default-ok | **Minimal** — reject non-object / empty body; do **not** require meta-schema compile success in this story (follow-up if needed) |
| G-R12 | **Registry persistence** | half-open | **In-memory only** (existing beans). Survives only for process lifetime. **C-3** = PostgreSQL tables |

## Platform / OpenAPI

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-R13 | **springdoc version for Boot 4.0.x** | resolved | **`3.0.3`** — see detail below |
| G-R14 | **Where springdoc / annotations live** | resolved | Match **qpointz** — see detail below |
| G-R15 | **Auth on `/registry` (and graph)** | resolved | **No auth** in this story — all `/api/v1/objs/**` endpoints are open |
| G-R19 | **Controller unit tests** | resolved | **Mandatory** — each controller WI ships MockMvc/`@WebMvcTest` (or equivalent) unit tests in the **same WI commit**; not optional, not deferred to “later” |

## Intentionally out of scope

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-R16 | Load-all / dump entire graph | out of scope | Use filtered GET `/graph` only |
| G-R17 | Per-resource `/entities`, `/edges`, `/subgraphs` | out of scope | All graph I/O via `/graph` |
| G-R18 | Catalog DB persistence | out of scope | Backlog **C-3** |

---

## G-R1 detail — annotation filter on GET `/graph`

**Status:** default-ok (2026-07-28)

| Decision | Choice |
|----------|--------|
| Encoding | Repeated query parameters: each `key=value` pair is one annotation constraint |
| Matching | Existing **match-all** semantics (`MatchAllAnnotationMatcher`) |
| Reserved params | None beyond the annotation map; do not use a nested `annotations[key]` form in this story |
| Example | `GET /api/v1/objs/graph?env=prod&tier=api` → filter `{env=prod, tier=api}` |

If a future need arises for keys that collide with framework params, introduce an explicit prefix (e.g. `a.env=prod`) in a follow-up.

## G-R3 detail — DELETE `/graph` body

**Status:** default-ok (2026-07-28)

```json
{
  "entityIds": ["…uuid…"],
  "edgeIds": ["…uuid…"]
}
```

- Both arrays optional but **at least one id total** required; else `400`.
- Entity delete uses existing store behaviour (entity + incident edges).
- Edge ids delete those edges only.
- Order: delete edges first (optional), then entities — or rely on store cascade for entities; document chosen order in WI-001/WI-002.

## G-R6 note — validate HTTP status

Prefer **`200` + result body** for dry-run so OpenAPI clients always parse `issues`. If team prefers REST-purist `400` when `isValid == false`, flip this gap before WI-002 ships and update OpenAPI accordingly.

## G-R13 detail — springdoc for Boot 4

**Status:** resolved (2026-07-28)

| Decision | Choice |
|----------|--------|
| Line | **springdoc-openapi `3.x`** (matrix: Boot `4.x` ↔ springdoc `3.x`; not `2.x`) |
| Pin | **`3.0.3`** — catalog key `springDoc` (same as qpointz); artifact `org.springdoc:springdoc-openapi-starter-webmvc-ui` |
| Placement | See **G-R14** (service + app `implementation`, not annotations-only) |
| Catalog | Add `springDoc = "3.0.3"` and `springdoc-openapi-starter-webmvc-ui` to [`libs.versions.toml`](../../../../libs.versions.toml) in **WI-004** |
| Verify | `./gradlew :objs-app:run` → Swagger UI + `GET /v3/api-docs` |

Source: [springdoc FAQ compatibility matrix](https://springdoc.org/faq.html), [springdoc-openapi v3.0.3](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.0.3), qpointz `libs.versions.toml` / WI-204.

## G-R14 detail — springdoc placement (qpointz pattern)

**Status:** resolved (2026-07-28) — aligned with [`qpointz`](../../../../../qpointz/qpointz)

Earlier draft used `compileOnly` swagger-annotations on the service and springdoc only on the app. **Superseded** by how Mill does it:

| Decision | Choice |
|----------|--------|
| Catalog | `springDoc = "3.0.3"` → `libs.springdoc.openapi.starter.webmvc.ui` (no hardcoded `org.springdoc:*:version` in build scripts) |
| Service module | `:objs-service` gets **`implementation(libs.springdoc.openapi.starter.webmvc.ui)`** — same as `mill-*-service` / `mill-service-common` |
| App module | `:objs-app` also **`implementation(...)`** — same as `apps/mill-service` |
| Annotations | `io.swagger.v3.oas.annotations.*` on controllers/DTOs; **no** separate `swagger-annotations-jakarta` `compileOnly` line (comes transitively via springdoc) |
| Grouping (optional) | Small `OpenApiConfiguration` with `GroupedOpenApi` for path prefixes (e.g. graph vs registry), like [`OpenApiConfiguration`](../../../../../qpointz/qpointz/services/mill-service-common/src/main/java/io/qpointz/mill/service/configuration/OpenApiConfiguration.java) in `mill-service-common` |

Reference modules: `services/mill-service-common/build.gradle.kts`, `metadata/mill-metadata-service/build.gradle.kts`, `apps/mill-service/build.gradle.kts`.

---

## Resolution log

| Gap | Decision | Date | Where recorded |
|-----|----------|------|----------------|
| G-R1 | Query params = annotation match-all filter | 2026-07-28 | this file |
| G-R2 | Empty filter → `400` | 2026-07-28 | this file |
| G-R3 | DELETE body `entityIds` / `edgeIds` | 2026-07-28 | this file |
| G-R4 | Batch delete all-or-nothing | 2026-07-28 | this file |
| G-R5 | PUT returns graph with ids | 2026-07-28 | this file |
| G-R6 | Validate → `200` + `BoMValidationResult` | 2026-07-28 | this file (default; may flip) |
| G-R7 | Schema PUT upsert | 2026-07-28 | this file |
| G-R8 | Missing DELETE → `404` | 2026-07-28 | this file |
| G-R9 | `/registry/edges` DELETE via query triple | 2026-07-28 | this file |
| G-R10 | Plain path segments | 2026-07-28 | this file |
| G-R11 | Minimal schema-body checks only | 2026-07-28 | this file |
| G-R12 | In-memory; C-3 later | 2026-07-28 | this file; BACKLOG C-3 |
| G-R13 | springdoc-openapi **3.0.3** (Boot 4.x line) | 2026-07-28 | this file (G-R13 detail); WI-004 |
| G-R14 | springdoc `implementation` on **service + app** (qpointz); no compileOnly-annotations-only | 2026-07-28 | this file (G-R14 detail); WI-004 |
| G-R15 | **No auth** on graph/registry HTTP | 2026-07-28 | this file |
| G-R16–G-R18 | Deferred / out of scope | 2026-07-28 | this file |
| G-R19 | Controller **unit tests mandatory** with WI-002 / WI-003 | 2026-07-28 | this file; WI-002, WI-003, WI-005 |
