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

Java: four-space indentation, `PascalCase` classes. Prefer Lombok (`@Slf4j`, `@Getter`) over boilerplate. Package root: `org.poc.objs` (scaffold may still use `io.qpointz.poc.objs` until renamed).

## Testing Guidelines

JUnit Jupiter + Mockito. Name tests `<Subject>Test`; methods `shouldX_whenY`. Prefer Spring Boot slice tests for web/JPA when adding features.

## Branching Strategy

Each story uses a **dedicated branch**, usually from `origin/dev`:
`git fetch origin && git checkout -b <story-slug> origin/dev`.
Rebase onto `origin/dev` before push. Never commit directly to `dev`.

## Stories & Work Items

Stories under `docs/workitems/planned/<story-slug>/` or `docs/workitems/in-progress/<story-slug>/`.
Normative process: [`docs/workitems/RULES.md`](docs/workitems/RULES.md).

## Commit Guidelines

Bracketed prefixes: `[feat]`, `[fix]`, `[change]`, `[refactor]`, `[docs]`, `[wip]`.
Imperative summary under 72 characters. Never add `Co-Authored-By` trailers.
