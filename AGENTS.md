Repository Guidelines
=====================

## Project Structure & Module Organization

Gradle multi-module Kotlin project. Foundation leaf modules at the repository root; concrete apps under `examples/`:

- `objs-core` — Entity SDK, domain types, JPA / PostgreSQL persistence, typed-domain toolkit
- `objs-service` — Spring REST API and Boot autoconfiguration (library); **foundation side service** (with UI)
- `objs-service-ui` — Workbench SPA (Vite/React); node-gradle build; JAR packs `static/workbench/`
- `objs-gremlin-core` — BoM → TinkerGraph materialization + gremlin-lang evaluation
- `objs-gremlin-service` — Gremlin REST (`POST /api/v1/objs/graph/traverse/gremlin`) autoconfiguration
- `objs-service-app` — Workbench-only runnable (`./gradlew :objs-service-app:run`, port **8081**) — objs-service + objs-service-ui; **no** example dependencies; **must not** be used by example apps
- `examples/sbom/sbom-service` (`:sbom-service`) — SBOM inventory app (launchable; programmatic objs-core only; port **8080**)
- `examples/sbom/sbom-service-ui` (`:sbom-service-ui`) — Inventory SPA; same node-gradle packaging as `:objs-service-ui`
- `examples/asset-repository/` — Asset repository example (`:asset-repository-service` + `:asset-repository-service-ui`); objs as object store — see [`docs/design/asset-repository/example.md`](docs/design/asset-repository/example.md)

Keep example apps in sync with foundation features — see [`docs/workitems/RULES.md`](docs/workitems/RULES.md) **Concrete example integration**.

Production sources: `src/main/kotlin`. Tests: `src/test/kotlin` (integration suites under `src/testIT/kotlin` when present).

## Build, Test, and Development Commands

- `./gradlew build` — compile, test, assemble
- `./gradlew test` — unit tests (all leaf modules)
- `./gradlew :objs-core:test` / `./gradlew :objs-service:test` — scoped tests
- `./gradlew :objs-core:testIT` — integration tests when defined
- `./gradlew :objs-service-app:run` — workbench only (H2, port 8081; `/workbench/` + `/api/v1/objs/**`)
- `./gradlew :sbom-service:run` — SBOM inventory example (H2, port 8080; inventory UI `/sbom/`; must not call objs-service)
- `./gradlew :asset-repository-service:run` — asset repository example (demo profile; domain UI `/ar/`)
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
