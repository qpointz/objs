# Objs

Entity/graph application (PoC). Multi-module Gradle **Kotlin** project.

## Modules

| Module | Path | Role |
|--------|------|------|
| `objs-core` | `objs-core/` | Entity SDK, validation, JPA / Flyway |
| `objs-service` | `objs-service/` | Spring REST + Boot autoconfiguration (library) |
| `objs-app` | `objs-app/` | Runnable assembly |

**Group / packages:** `org.poc.objs` · **JVM toolchain:** 24 (Kotlin) · **Spring Boot:** 4.x

Domain design: [`docs/design/graph/`](docs/design/graph/README.md).  
Story: [`docs/workitems/completed/20260728-entity-graph-foundation/`](docs/workitems/completed/20260728-entity-graph-foundation/STORY.md).

## Build

```bash
./gradlew test
./gradlew :objs-core:build :objs-service:build :objs-app:build
./gradlew :objs-app:run
```

Version comes from the root `VERSION` file (override with `-PprojectVersion=`).

## Agent workflow

See [`AGENTS.md`](AGENTS.md) and [`docs/workitems/RULES.md`](docs/workitems/RULES.md).
