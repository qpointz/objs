# Gaps — objs-core-spring-split (C-25)

Status: `open` | `resolved` | `deferred` | `cancelled` | `accepted-risk`.

**WI-001:** all design rows below are **resolved** or explicitly deferred/cancelled. Implementation WIs may proceed after WI-001 docs/tracker pass.

**Backlog id:** **C-25** (keep **C-24** for `objs-policy`; WI-000 incorrectly reused C-24 — tracker hygiene in WI-001).

**Story principles**
- **Behaviour unchanged** — store/DAO method surface and mutate/select semantics stay; no product redesign.
- **Maxim** — *“I can do everything with `:objs-api` except persistence”* (and except running networknt `Validator` until G-X8).
- **Pattern** — `*-api` + Spring-free persistence module + tiny `*-autoconfigure`.
- **Names** — Gradle **`:objs-persistence`** = persistence role (renamed from `:objs-core`; **G-X7** resolved). Packages remain `org.poc.objs.core.*`.
- **TX** — internal UoW in persistence; hidden from api / REST / gremlin / store signatures.

---

## Open (design)

_None._

---

## Resolved

| # | Topic | Status | Resolution |
|---|--------|--------|------------|
| G-A1 | DAO granularity | **resolved** | 1:1 mirror of 12 repos; no aggregate redesign. |
| G-A2 | TX / UoW | **resolved** | Internal UoW (`read` / `write` / `writeNew` + after-rollback); hidden from api layer. |
| G-A3 | Spring TX | **resolved** | `TransactionTemplate` in autoconfigure. |
| G-A4 | Test harness | **resolved** | Model tests → `:objs-api`; `ObjsPersistenceTestSupport` in core `src/test` (EMF + objs Flyway + EM UoW); Postgres `testIT` on core; Boot slices → autoconfigure; no separate test module. |
| G-A5 | Configuration properties | **resolved** | Plain settings in **core**; `@ConfigurationProperties` **only** in autoconfigure; same prefixes (`objs.catalogs`, `objs.flyway`, `objs.seeds`). |
| G-A6 | Export | **resolved** | Add `objs-autoconfigure` to dumper lists → `platform-autoconfigure`. Module dir `objs-persistence` → `platform-persistence` (G-X7). |
| G-A7 | Seeds / `ResourceLoader` | **resolved** | Parse/SPI → api; apply/ledger → persistence; startup + Spring `ResourceLoader` → autoconfigure; api `(String) -> InputStream` / `SeedResourceResolver`. |
| G-A8 | Hibernate without Boot | **resolved** | Persistence: `jakarta.persistence-api` + `hibernate-core` + Flyway **core**; no Boot BOM; EMF in tests / non-Spring wiring; keep `@JdbcTypeCode`. |
| G-A9 | Module names | **resolved** | `:objs-autoconfigure` + `:objs-persistence` (G-X7 rename done). |
| G-A10 | Consumer deps | **resolved** | Boot → autoconfigure; engines → api. |
| G-A11 | Gremlin | **resolved** | `:objs-gremlin-core` → **`:objs-api` only**. |
| G-A12 | Flyway SQL location | **resolved** | SQL in core JAR; autoconfig ordering. |
| G-A13 | No Spring Data | **resolved** | Delete `JpaRepository`; no shim. |
| G-A14 | Store semantics | **resolved** | No REST/mutate behaviour change. |
| G-A15 | Store ports | **resolved** | Ports on api; impls in core; surface wide enough that gremlin never imports core (per G-A20). |
| G-A16 | `@Lazy` cycle | **resolved** | Break `NamedGraphStore` ↔ `GraphStore` with ports / ctor order / setter; no Spring `@Lazy`. |
| G-A17 | JDBC without Spring | **resolved** | Replace `JdbcTemplate` / `DataSourceUtils` with `Connection` joined to active UoW/TX. |
| G-A18 | Flyway `{vendor}` | **resolved** | JDBC-URL → `postgresql` \| `h2` map in core; fail fast; no Boot `DatabaseDriver`. |
| G-A19 | Topology / pattern | **resolved** | api + `:objs-persistence` + autoconfigure (G-X7 rename done). |
| G-A20 | Api package move set | **resolved** | Boundary table below. Matcher hybrid C; validation hybrid C. |
| G-A21 | Api new dependencies | **resolved** | **JEXL** on `:objs-api` only. No networknt / Jackson 2 on api. |

### G-A20 locked boundary

| → `:objs-api` | → `:objs-persistence` | → `:objs-autoconfigure` |
|---------------|------------------------------|-------------------------|
| Graph primitives (already) | JPA records, DAOs | Autoconfig classes |
| Schema / allow-list interfaces + **InMemory** catalogs | **Jpa** catalogs (Caffeine write-through) | `@ConfigurationProperties` binders |
| Validation **contracts** (`ValidationResult`, issues, PersistGate port) | **Validator** impl (networknt + Jackson 2 tree); store **impls** (`GraphStore`, `NamedGraphStore`, …) | UoW Spring impl |
| **Matcher contract** + **in-memory / JEXL eval** | **Matcher persistence strategies** (SQL pushdown, `PoolEntityReader`, store-backed select) | Seed startup + `ResourceLoader` |
| Seed **parse** / document SPI / YAML | Seed **apply**, ledger, graph seed handler → store | Explicit `@Bean` wiring |
| Store **ports** | Flyway SQL + vendor helper | Catalog hydrate / seed `ApplicationRunner`s |
| VersioningStrategy, identity projection, merge policy, JSON Schema export (catalog-facing) | Deep version **persistence** service | |
| Typed helpers already in api; `RegistryPack` if non-JPA | | |

**Straddle rule:** EM / JDBC / Flyway / store impls / networknt → **core**; domain types / catalogs / in-memory graphs / matcher in-memory eval → **api**.

---

## Out of story

| # | Topic | Status | Notes |
|---|--------|--------|-------|
| G-X1 | `store-text-search` | **deferred** | C-20 |
| G-X2 | Non-JPA persistence | **deferred** | Future backend |
| G-X3 | jgrapht merge | **deferred** | Closed on `dev` |
| G-X4 | Spring-free objs-service | **deferred** | REST stays MVC |
| G-X5 | `:objs-core-jpa` | **cancelled** | Superseded by G-A19 |
| G-X6 | Rename GraphStore / NamedGraphStore | **deferred** | Naming confusion only |
| G-X7 | Rename `:objs-core` → `:objs-persistence` | **resolved** | Gradle module + export + living docs; packages stay `org.poc.objs.core.*` |
| G-X8 | Validator on api (Jackson 3) | **deferred** | Move networknt/`Validator` to api when Jackson 3-capable |

---

## DAO map (G-A1)

| Current interface | Record | Notes |
|-------------------|--------|-------|
| `EntityRepository` | `EntityRecord` | → JPQL |
| `EdgeRepository` | `EdgeRecord` | → JPQL |
| `GraphRepository` | `GraphRecord` | |
| `GraphMembershipRepository` | `GraphMembershipRecord` | |
| `EntityVersionRepository` | `EntityVersionRecord` | Drop `Pageable` |
| `GraphVersionRepository` | `GraphVersionRecord` | |
| `EdgeVersionRepository` | `EdgeVersionRecord` | Drop `Pageable` |
| `GraphVersionMemberRepository` | `GraphVersionMemberRecord` | |
| `GraphVersionEdgeRepository` | `GraphVersionEdgeRecord` | |
| `SchemaCatalogRepository` | schema catalog row | |
| `AllowedEdgeRuleRepository` | allow-list row | |
| `SeedLedgerRepository` | seed ledger row | |
