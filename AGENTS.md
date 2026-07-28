Repository Guidelines
=====================

## Project Structure & Module Organization

Gradle multi-module Java project. Libraries live under domain folders:

- `core/objs-core` — Entity SDK, domain types, JPA / PostgreSQL persistence
- `services/objs-service` — Spring REST API and Boot autoconfiguration

Production sources: `src/main/java`. Tests: `src/test/java` (integration suites under `src/testIT/java` when present).

## Build, Test, and Development Commands

- `./gradlew build` — compile, test, assemble
- `./gradlew test` — unit tests (all leaf modules)
- `./gradlew :core:objs-core:test` / `./gradlew :services:objs-service:test` — scoped tests
- `./gradlew :services:objs-service:testIT` — integration tests when defined
- `./gradlew clean` — remove build outputs

## Coding Style & Naming Conventions

Java/Kotlin: four-space indentation, `PascalCase` classes. Prefer Lombok only if Java remains; new code is **Kotlin**. Package root: `org.poc.objs`. Domain types use `Bo` prefix (`BoEntity`, `BoEdge`).

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
