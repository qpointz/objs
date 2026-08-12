Repository Guidelines
=====================

## Project Structure & Module Organization

Gradle multi-module Kotlin project. Leaf modules at the repository root:

- `objs-core` — Entity SDK, domain types, JPA / PostgreSQL persistence, typed-domain toolkit
- `objs-service` — Spring REST API and Boot autoconfiguration (library)
- `objs-gremlin-core` — BoM → TinkerGraph materialization + gremlin-lang evaluation
- `objs-gremlin-service` — Gremlin REST (`POST /api/v1/objs/graph/traverse/gremlin`) autoconfiguration
- `objs-sbom-example` — Concrete SBOM app (canonical ontology + `/api/v1/example/sbom`); keep in sync with foundation features — see [`docs/workitems/RULES.md`](docs/workitems/RULES.md) **Concrete example integration**
- `objs-app` — Runnable assembly (`./gradlew :objs-app:run`)

Production sources: `src/main/kotlin`. Tests: `src/test/kotlin` (integration suites under `src/testIT/kotlin` when present).

## Build, Test, and Development Commands

- `./gradlew build` — compile, test, assemble
- `./gradlew test` — unit tests (all leaf modules)
- `./gradlew :objs-core:test` / `./gradlew :objs-service:test` — scoped tests
- `./gradlew :objs-core:testIT` — integration tests when defined
- `./gradlew :objs-app:run` — run the service locally (H2)
- `./gradlew clean` — remove build outputs

## Coding Style & Naming Conventions

Java/Kotlin: four-space indentation, `PascalCase` classes. Prefer Lombok only if Java remains; new code is **Kotlin**. Package root: `org.poc.objs`. Domain types use `BoM` prefix (Bill of Materials: `BoMEntity`, `BoMEdge`).

## Testing Guidelines

JUnit Jupiter + Mockito. Name tests `<Subject>Test`; methods `shouldX_whenY`. Prefer Spring Boot slice tests for web/JPA when adding features.

## Branching Strategy

Each story uses a **dedicated branch**, usually from `dev` / `origin/dev`:
`git fetch origin && git checkout -b <story-slug> origin/dev`
(or from local `dev` if no remote yet).
Rebase onto `dev` / `origin/dev` before push. Never commit directly to `dev`.

## Stories & Work Items

Stories under `docs/workitems/planned/<story-slug>/` or `docs/workitems/in-progress/<story-slug>/`.
Normative process: [`docs/workitems/RULES.md`](docs/workitems/RULES.md).

## Commit Guidelines

Bracketed prefixes: `[feat]`, `[fix]`, `[change]`, `[refactor]`, `[docs]`, `[wip]`.
Imperative summary under 72 characters. Never add `Co-Authored-By` trailers.
