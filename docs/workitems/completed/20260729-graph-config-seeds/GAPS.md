# Gaps & clarifications — graph-config-seeds

Open decisions for [`STORY.md`](STORY.md). Resolve each blocking item before its dependent work
item. Do not invent unresolved behavior silently in code.

**Current state:** Stage 1 accepted; Stage 2 in progress. Remaining Stage 2 defaults were
accepted at Stage 2 start (2026-07-29). No blocking gaps remain.

**Legend:** `blocking` = user decision required before implementation · `default-ok` = proposed
default may be accepted or changed · `resolved` = agreed

## Stage 1 — PostgreSQL persistence

| # | Topic | Status | Clarification |
|---|-------|--------|---------------|
| G-P1 | Stage 1 persistence scope | default-ok | Proposed: exercise and fix the existing entity/edge PostgreSQL path, then add registry persistence; do not limit Stage 1 to registry tables |
| G-P2 | Stage boundary | resolved | Stop after WI-002 so the user can perform manual PostgreSQL testing; begin Stage 2 only after explicit approval |
| G-P3 | Automated PostgreSQL tests | resolved | Use Testcontainers PostgreSQL for PostgreSQL-specific integration tests; retain fast H2 tests where they still provide useful coverage |
| G-P4 | Catalog / registry abstraction | resolved | Consumers use catalog interfaces/base types; production uses PostgreSQL-authoritative implementations with an in-memory write-through cache; see detail |
| G-P5 | Existing registry API compatibility | resolved | Replace raw JSON Schema registration with the authoritative object-schema DSL before seed v1; expose generated JSON Schema through a projection endpoint |

### G-P4 detail — registry abstraction

**Status:** resolved (2026-07-29)

| Decision | Choice |
|----------|--------|
| Abstraction | Define schema-catalog and allowed-edge-catalog interfaces/base types; put reusable matching/precedence behavior in shared base/helper code |
| In-memory implementation | Owns the fast runtime lookup structures and remains directly usable by tests and non-persistent configurations |
| Persistent implementation | Composes the in-memory implementation with JPA repositories; PostgreSQL is authoritative and the memory layer is its runtime cache |
| Reads | Resolve schemas and most-specific edge rules from memory; do not query PostgreSQL on every validation |
| Startup | Load all persisted schemas/rules into the cache before validation and HTTP traffic |
| Writes | Commit registration/removal to PostgreSQL, then update/invalidate memory after successful commit; failed transactions must not alter cache state |
| Consumers | Validator, REST registry, seeds, SBOM, OpenAPI, and typed toolkit depend only on catalog abstractions |
| Consistency boundary | This story assumes registry writes pass through the application abstraction; direct DB changes and multi-node cache invalidation are out of scope |
| Semantics | In-memory and persistent variants expose identical register/get/remove/list/find behavior, including most-specific rule matching |

## Stage 2 — Seed format and identity

| # | Topic | Status | Clarification |
|---|-------|--------|---------------|
| G-S1 | Seed graph identity | resolved | YAML uses stable textual keys; importer maps them to deterministic **UUIDv5** entity/edge ids so repeated imports are idempotent |
| G-S2 | Format envelope/version | resolved | Each YAML document has `apiVersion: objs.poc.org/v1` and a `kind`; reject unsupported versions |
| G-S3 | Initial document kinds | resolved | Extensible multi-document pack with `ObjectSchema`, `AllowedEdgeRule`, and `Graph` in v1 |
| G-S4 | Unknown document kinds | resolved | Fail the resource by default rather than silently skipping likely typos; future versions register new handlers |
| G-S5 | Merge and deletion semantics | resolved | Upsert schemas/rules/graph items by stable identity; omission never deletes; no `REPLACE` in v1 |
| G-S6 | Resource locations | resolved | Ordered `classpath:` and `file:` resources only in v1 |
| G-S7 | Seed failure behavior | resolved | `fail-fast` default with configurable `continue`; any document error prevents a successful ledger entry |

## Stage 2 — Ledger and API

| # | Topic | Status | Clarification |
|---|-------|--------|---------------|
| G-S8 | Fingerprint | resolved | SHA-256 over raw resource bytes; store algorithm prefix (`sha256:`) |
| G-S9 | Ledger key | resolved | Normalized resource location with credentials/query removed; resources require no separate name |
| G-S10 | REST import | resolved | Multipart YAML import using the same importer and structured per-document result |
| G-S11 | REST export scope | resolved | Export **all seed kinds** present in the system, including `Graph` seeds; keep the format/handler design open so new seed types can be added later; never expose an unbounded load-all graph dump — graph export remains annotation-filtered / otherwise bounded |
| G-S12 | SBOM source of truth | resolved | Canonical YAML becomes registry source of truth; retain typed Kotlin metadata/builders and enforce parity in tests |
| G-S13 | Demo graph | resolved | Convert the existing SBOM demo graph to a property-gated `Graph` seed |

## Object-schema DSL

| # | Topic | Status | Clarification |
|---|-------|--------|---------------|
| G-D1 | Authoritative representation | resolved | Persist the typed recursive DSL only; generate JSON Schema on demand |
| G-D2 | Facet semantics | resolved | Reuse the qpointz payload-schema DSL structure and capabilities, but do not introduce facets, applicability, cardinality, categories, or scopes |
| G-D3 | Type system | resolved | Support `OBJECT`, `ARRAY`, `STRING`, `NUMBER`, `INTEGER`, `BOOLEAN`, and `ENUM`; `INTEGER` is the required Objs/SBOM extension |
| G-D4 | Existing development data | resolved | No raw JSON Schema migration; recreate development data because seed v1 has not shipped |
| G-D5 | Seed schema kind | resolved | `ObjectSchema` embeds the same DSL; there is no parallel raw-JSON-Schema seed kind |

## Resolution log

| Gap | Decision | Date |
|-----|----------|------|
| G-P2 | Manual PostgreSQL acceptance gates Stage 2 | 2026-07-29 |
| G-P3 | Use Testcontainers PostgreSQL in addition to useful H2 tests | 2026-07-29 |
| G-P4 | Abstract catalogs; PostgreSQL-authoritative production implementation with in-memory write-through cache | 2026-07-29 |
| G-P5 | Make the object-schema DSL authoritative and expose JSON Schema as a generated projection | 2026-07-29 |
| G-S1 | Stable textual keys → deterministic UUIDv5 for seeded entities/edges | 2026-07-29 |
| G-S3 | Use an extensible, qpointz-style multi-document pack with `ObjectSchema` definitions | 2026-07-29 |
| G-S11 | Export all seed kinds including graphs; keep kind registry extensible; graph export stays bounded | 2026-07-29 |
| G-D1–G-D5 | Adopt and persist the qpointz-inspired object-schema DSL before defining seed v1 | 2026-07-29 |
| G-S2, G-S4–G-S10, G-S12–G-S13 | Accept Stage 2 defaults at Stage 2 start | 2026-07-29 |
| G-P2 | Stage 1 PostgreSQL acceptance confirmed; Stage 2 approved | 2026-07-29 |

