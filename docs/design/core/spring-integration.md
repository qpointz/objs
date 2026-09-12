# Spring Boot integration (`:objs-autoconfigure`)

**Status:** living how-to  
**Parent:** [`README.md`](README.md)  
**Modules:** `:objs-autoconfigure` (Boot adapter) · `:objs-persistence` (JPA store) · `:objs-api` (model)  
**Related:** [`transaction-recipes.md`](transaction-recipes.md) · [`spring-split.md`](spring-split.md) · [`../graph/programmatic-recipes.md`](../graph/programmatic-recipes.md) · [`../graph/persistence.md`](../graph/persistence.md)

How to embed objs in a Spring Boot application. Transaction join semantics (app `@Transactional` + objs stores) live in [`transaction-recipes.md`](transaction-recipes.md). Module boundaries / C-25 history: [`spring-split.md`](spring-split.md).

## When to use

| Consumer | Depend on | Notes |
|----------|-----------|--------|
| Boot app (workbench, SBOM, asset-repository, your service) | `:objs-autoconfigure` | Pulls `:objs-persistence`; wires UoW, stores, objs Flyway |
| Optional REST surface | `:objs-service` (and/or gremlin/policy service modules) | Still needs autoconfigure for the store |
| Non-Spring / plain Hibernate | `:objs-persistence` only | Manual EMF + [`EntityManagerUnitOfWork`](transaction-recipes.md#non-spring) |

## Gradle

```kotlin
dependencies {
    implementation(project(":objs-autoconfigure"))
    // optional HTTP:
    // implementation(project(":objs-service"))
}
```

Ensure Boot JDBC + JPA + transaction starters are on the classpath (typical `spring-boot-starter-data-jpa`). Autoconfigure expects a `DataSource`, JPA `EntityManager`, and `PlatformTransactionManager`.

## What autoconfig registers

### `ObjsCoreAutoConfiguration`

- `@EntityScan` / `@AutoConfigurationPackage` for `org.poc.objs.core.persistence` (objs JPA types)
- `@EnableTransactionManagement`
- **Unit of work:** `TransactionTemplateUnitOfWork` (Spring `TransactionTemplate`; see [transaction-recipes](transaction-recipes.md))
- **DAOs** + **stores:** `GraphStore`, `NamedGraphStore`, version DAOs, catalog DAOs, …
- **Catalogs:** `JpaSchemaCatalog` / `JpaAllowedEdgeCatalog` (write-through + Caffeine TTL)
- **Validation / versioning:** `Validator`, default `ExplicitOnlyVersioningStrategy`
- **Seeds:** handlers, `SeedImporter`, `SeedLedger`, `SeedStartupLoader` + startup `ApplicationRunner`s (catalog hydrate, then configured seeds)
- Default no-op `GraphVersionReferenceGuard` (`AllowAll…`) — override if external refs must block purge/destroy

Beans are `@ConditionalOnMissingBean` — apps may replace UoW, stores, catalogs, versioning, or the guard.

### `ObjsFlywayAutoConfiguration`

Second Flyway line for `objs_*` DDL (`flyway_schema_history_objs`). Runs **before** Boot Flyway and before Hibernate validate. Enabled by default (`objs.flyway.enabled`, match-if-missing).

See [`../graph/persistence.md`](../graph/persistence.md) for the two-line Flyway model.

## Application configuration

Minimal:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/app
    username: …
    password: …
  jpa:
    hibernate:
      ddl-auto: validate
```

Objs properties (optional):

| Prefix | Purpose | Defaults (sketch) |
|--------|---------|-------------------|
| `objs.flyway` | Objs migration line | `enabled=true`, `table=flyway_schema_history_objs`, vendor locations under `classpath:org/poc/objs/core/db/migration/{vendor}` |
| `objs.seeds` | Startup seed import | `enabled=true`, `onFailure=FAIL_FAST`, `resources=[]` |
| `objs.catalogs` | Catalog read-cache TTL | `cache-ttl=30s` (`PT0S` disables TTL expiry) |

Example seed locations:

```yaml
objs:
  seeds:
    enabled: true
    resources:
      - classpath:seeds/ontology.yaml
```

## App entities alongside objs

For **one** Spring transaction to cover both inventory tables and `objs_*`:

1. Same `DataSource` and `PlatformTransactionManager`
2. Same JPA persistence unit — scan **both** objs packages (autoconfig) and your app entity packages (`@EntityScan` / Boot entity scan)
3. App Spring Data repos: `@EnableJpaRepositories(basePackages = "…your.app.persistence")`

SBOM does this: inventory `JpaRepository` types + objs `GraphStore` / `NamedGraphStore` under `@Transactional` services.

## Inject stores, not DAOs

```kotlin
@Service
class MyGraphService(
    private val namedGraphs: NamedGraphStore,
    private val graphStore: GraphStore,
) { /* … */ }
```

`*Dao` beans exist for extension; stores own `uow.read` / `uow.write`. Graph call recipes: [`../graph/programmatic-recipes.md`](../graph/programmatic-recipes.md).

Optional: provide a `GraphVersionReferenceGuard` bean so purge/destroy fail closed when versions are referenced outside objs (e.g. SBOM fingerprints).

## Overrides

Common replacements via `@Bean` + `@ConditionalOnMissingBean` already on the autoconfig side:

- Custom `UnitOfWork` (rarely needed — prefer the shipped Spring adapter)
- In-memory `SchemaCatalog` / `AllowedEdgeCatalog` for tests
- Custom `VersioningStrategy`
- Custom `GraphVersionReferenceGuard`

## Do not

- Construct `EntityManagerUnitOfWork` inside a Boot app that uses `@Transactional` — that UoW opens its **own** EM/TX and will **not** join Spring
- Give objs a second `DataSource` / EMF and expect shared `@Transactional` without XA
- Call DAOs from app code and skip store facades (easy to miss UoW boundaries and validation)

## Related

- [transaction-recipes.md](transaction-recipes.md) — Spring join / non-Spring UoW / pitfalls
- [spring-split.md](spring-split.md) — why persistence is Spring-free
- [persistence-backends.md](persistence-backends.md) — future backends
- [../graph/programmatic-recipes.md](../graph/programmatic-recipes.md) — store call cookbook
- [../graph/persistence.md](../graph/persistence.md) — schema + Flyway
