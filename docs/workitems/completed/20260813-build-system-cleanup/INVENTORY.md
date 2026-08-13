# Build dependency & plugin inventory (P-1 / WI-001)

Decisions for WI-002…WI-004. Status as of story start on `origin/dev`.

## Plugins

| Plugin | Where applied | Decision | Notes |
|--------|---------------|----------|-------|
| `org.jetbrains.kotlin.jvm` | all Kotlin modules + root apply false | **keep** | Required |
| `org.jetbrains.kotlin.plugin.spring` | core, service, sbom, gremlin-service, app | **keep** (those modules only) | All-open for Spring proxies; do not hand-`open` |
| `org.jetbrains.kotlin.plugin.jpa` | objs-core only | **keep** (core only) | No-arg for `@Entity` |
| `io.spring.dependency-management` | all leaf modules | **drop** | Replace with `platform(libs.boot.dependencies)` |
| `org.springframework.boot` | catalog only | **drop from catalog** | Never applied |
| `org.jsonschema2pojo` | objs-sbom-example | **drop** | Generated types unused by main model; only smoke test |
| `java-library` / `application` / `base` | as today | **keep** | |
| foojay-resolver-convention | settings | **keep** | |
| jacoco | root via `java-library` | **keep** | |
| Root Kotlin `apply false` | root | **drop** | Catalog aliases version plugins in subprojects |

### Target plugin set

| Module | Plugins |
|--------|---------|
| objs-core | `java-library`, `kotlin-jvm`, `kotlin-spring`, `kotlin-jpa` |
| objs-service | `java-library`, `kotlin-jvm`, `kotlin-spring` |
| objs-sbom-example | `java-library`, `kotlin-jvm`, `kotlin-spring` |
| objs-gremlin-core | `java-library`, `kotlin-jvm` |
| objs-gremlin-service | `java-library`, `kotlin-jvm`, `kotlin-spring` |
| objs-app | `application`, `kotlin-jvm`, `kotlin-spring` |

## Catalog libraries

| Entry | Decision | Notes |
|-------|----------|-------|
| `lombok` | **drop** | Unused |
| `boot-starter-jackson` | **drop** | Unused |
| `boot-dependencies` | **keep** | Platform BOM |
| `boot-configuration-processor` | **drop** | Declared on service; no kapt; props live in core |
| Boot starters (webmvc, data-jpa, flyway, test, …) | **keep** | Used |
| `flyway-core` | **drop** | Redundant with `boot-starter-flyway` |
| `flyway-postgresql` | **keep** | App / IT runtime |
| `jackson-core` | **drop** | Not referenced; transitive via databind |
| `jackson-databind` / `yaml` / `module-kotlin` | **keep** | Core API / seeds |
| `jackson-annotations-v2` | **drop** | Only for jsonschema2pojo path |
| `bundles.jackson` | **keep** | databind + yaml + module-kotlin |
| `bundles.logging` + slf4j/logback | **drop** | Unused direct logging; Boot brings SLF4J |
| `json-schema-validator` | **keep** | BoMValidator |
| `commons-jexl` | **keep** | Matchers |
| `gremlin-core` / `tinkergraph` / `gremlin-language` | **keep** | gremlin-core module |
| `gql-gremlin` | **drop** | Declared; no source imports |
| `junit-jupiter-api` / `engine` | **keep** | gremlin-core tests (no Boot test starter) |
| `mockito-*` | **drop from catalog** | Covered by `boot-starter-test` where used |
| `assertj-core` | **keep** | Needed for gremlin-core; optional elsewhere via Boot test |
| `springdoc-openapi-…` | **keep** | OpenAPI surface; **`api`** on objs-service so downstream controllers compile |
| `kotlin-stdlib` | **drop from catalog** | Kotlin JVM plugin adds it |
| `kotlin-reflect` | **keep** | Spring / Jackson |
| `h2` / `postgresql` / testcontainers | **keep** | Pin `postgresql` version in catalog |
| `jsonschema2pojo` version | **drop** | Plugin removed |

## Per-module dependency actions

### objs-core
- Add `api(platform(libs.boot.dependencies))`
- Keep: boot starters (starter, data-jpa, flyway), jackson bundle, kotlin-reflect, json-schema-validator, commons-jexl
- Drop: flyway-core, logging bundle, explicit kotlin-stdlib
- Tests: boot test starters + h2; drop explicit mockito (use Boot); keep assertj optional or via Boot; IT: postgresql, flyway-postgresql, testcontainers

### objs-service
- Rely on core’s platform (or local `api(platform)` if needed)
- Keep: project core, boot-starter-webmvc, kotlin-reflect, springdoc as **`api`**
- Drop: jackson-module-kotlin (via core API), configuration-processor, kotlin-stdlib
- Remove empty `testIT` suite registration
- Tests: boot webmvc test (+ AssertJ/Mockito via Boot)

### objs-sbom-example
- Keep: project core + service, kotlin-reflect
- Drop: jackson-* redeclares, springdoc (via service), kotlin-stdlib, jsonschema2pojo plugin + gen-on-compile
- Remove `GeneratedSbomModelTest`; keep `exportSbomJsonSchema` task + committed schema JSON

### objs-gremlin-core
- Keep: project core, gremlin-core, tinkergraph, gremlin-language, kotlin-reflect
- Drop: gql-gremlin, logging bundle, kotlin-stdlib, Spring DM / local BOM if platform flows from core
- Tests: junit + assertj (no Boot)

### objs-gremlin-service
- Keep: project gremlin-core + service, kotlin-reflect
- Drop: boot-starter-webmvc, springdoc, jackson-module-kotlin, kotlin-stdlib (transitive from service)

### objs-app
- Keep: project service + sbom + gremlin-service, kotlin-reflect, runtime h2/postgresql/flyway-postgresql
- Drop: springdoc, kotlin-stdlib
