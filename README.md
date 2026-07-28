# Objs

Entity/graph application (PoC). Multi-module Gradle Java project.

## Modules

| Module | Path | Role |
|--------|------|------|
| `objs-core` | `core/objs-core` | Entity SDK, core types, JPA / PostgreSQL persistence |
| `objs-service` | `services/objs-service` | Spring REST + Boot autoconfiguration |

**Group / packages (target):** `org.poc.objs` · **Scaffold today:** `io.qpointz.poc.objs` · **Java:** 25 · **Spring Boot:** 4.x (via catalog)

Domain design: [`docs/design/graph/`](docs/design/graph/README.md).  
First story: [`docs/workitems/planned/entity-graph-foundation/`](docs/workitems/planned/entity-graph-foundation/STORY.md).

## Build

```bash
./gradlew test
./gradlew :core:objs-core:build :services:objs-service:build
```

Version comes from the root `VERSION` file (override with `-PprojectVersion=`).

## Agent workflow

See [`AGENTS.md`](AGENTS.md) and [`docs/workitems/RULES.md`](docs/workitems/RULES.md).
