# Milestones

**Draft release notes.** Treat this file as the **working draft** of **`releases/RELEASE-x.y.z.md`**
for the **next** version only. See [`RULES.md`](RULES.md) § **Milestone ledger (`MILESTONE.md`)** and
§ **Release (version) process**.

## 0.1.0

**Target date:** TBD — **not released.**

### Completed

- Project scaffold — Gradle multi-module shell (`objs-core`, `objs-service`), workitem process docs.
- [`entity-graph-foundation`](completed/20260728-entity-graph-foundation/STORY.md) — Kotlin entity store: `BoMEntity`/`BoMEdge`, annotations/subgraphs, schema + allow-list (`*` wildcards), persist gate, Flyway/JPA (H2), packages `org.poc.objs`; modules flattened; `:objs-app` runnable assembly.

### In Progress

- [`entity-rest-api`](in-progress/entity-rest-api/STORY.md) — `/graph` + `/registry` REST, OpenAPI (springdoc in `:objs-app` / service); catalogs remain in-memory until C-3.

### Planned

_(none)_
