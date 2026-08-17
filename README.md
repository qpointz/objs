# Objs

Entity/graph application (PoC). Multi-module Gradle **Kotlin** project.

## Modules

| Module | Path | Role |
|--------|------|------|
| `objs-core` | `objs-core/` | Entity SDK, validation, JPA / Flyway |
| `objs-service` | `objs-service/` | Foundation REST + Boot autoconfiguration (library) |
| `objs-service-ui` | `objs-service-ui/` | Foundation workbench SPA |
| `objs-gremlin-core` | `objs-gremlin-core/` | In-process Gremlin evaluation |
| `objs-gremlin-service` | `objs-gremlin-service/` | Gremlin REST autoconfiguration |
| `objs-service-app` | `objs-service-app/` | **Workbench runner** (`:objs-service-app:run`, port 8081) |
| `sbom-service` | `examples/sbom/sbom-service/` | SBOM inventory app (port 8080; **no** objs-service) |
| `sbom-service-ui` | `examples/sbom/sbom-service-ui/` | Inventory SPA |

**Group / packages:** `org.poc.objs` · **JVM toolchain:** 21 (Kotlin) · **Spring Boot:** 4.x

Domain design: [`docs/design/graph/`](docs/design/graph/README.md).  
Story: [`docs/workitems/completed/20260816-sbom-inventory-app/`](docs/workitems/completed/20260816-sbom-inventory-app/STORY.md).

`:sbom-service` **must not** compile against `:objs-service` / `:objs-service-ui` — those ship via `:objs-service-app` (or a demo `runtimeOnly` sidecar).

## Build

```bash
./gradlew test
./gradlew :objs-core:build :objs-service:build :objs-service-app:build
./gradlew :objs-service-app:run  # workbench only → :8081
./gradlew :sbom-service:run      # SBOM inventory → :8080
```

Version comes from the root `VERSION` file (override with `-PprojectVersion=`).

## Agent workflow

See [`AGENTS.md`](AGENTS.md) and [`docs/workitems/RULES.md`](docs/workitems/RULES.md).
