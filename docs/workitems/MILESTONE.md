# Milestones

**Draft release notes.** Treat this file as the **working draft** of **`releases/RELEASE-x.y.z.md`**
for the **next** version only. See [`RULES.md`](RULES.md) § **Milestone ledger (`MILESTONE.md`)** and
§ **Release (version) process**.

## 0.1.0

**Target date:** TBD — **not released.**

### Completed

- Project scaffold — Gradle multi-module shell (`objs-core`, `objs-service`), workitem process docs.
- [`entity-graph-foundation`](completed/20260728-entity-graph-foundation/STORY.md) — Kotlin entity store: `BoMEntity`/`BoMEdge`, annotations/subgraphs, schema + allow-list (`*` wildcards), persist gate, Flyway/JPA (H2), packages `org.poc.objs`; modules flattened; `:objs-app` runnable assembly.
- [`entity-rest-api`](completed/20260728-entity-rest-api/STORY.md) — `/graph` + `/registry` REST, springdoc OpenAPI 3.0.3, MockMvc tests; `BoM*` rename; catalogs in-memory until C-3.
- [`sbom-typed-example`](completed/20260728-sbom-typed-example/STORY.md) — typed toolkit; full canonical ontology (A–D); `/api/v1/example/sbom`; Python bulk seed; graph explorer SPA at `/ui/`.
- [`graph-config-seeds`](completed/20260729-graph-config-seeds/STORY.md) — PostgreSQL-authoritative catalogs; object-schema DSL and authoring workbench; lazy/pushable graph reads; flat multi-document seed import/export with durable startup ledger; canonical SBOM YAML.

### In Progress

_(none)_

### Planned

_(none)_
