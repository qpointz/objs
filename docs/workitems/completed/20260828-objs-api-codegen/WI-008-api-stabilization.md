# WI-008 — Renamed API stabilization and migration gate

**Story:** [`STORY.md`](STORY.md)
**Stage:** 3 — Renamed API stabilization
**Status:** completed
**Depends on:** WI-002
**Implementation commit:** [`420b54c`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/420b54c)

## Goal

Stabilize the extracted, unprefixed `objs-api` contract before introducing schema export or code
generation. This WI is the migration gate between the destructive runtime reshuffle and all later
behavioral work.

## Scope

- Run repository-wide package, symbol, and dependency checks after the public rename
- Verify `objs-api` remains Spring-free, JPA-free, persistence-free, and Java-compatible
- Verify root `objs-*` modules remain schema/ontology-agnostic and depend on the API in the allowed
  direction
- Verify caller-supplied Jackson configuration and the absence of a hidden mapper
- Verify UUID-only identity, duplicate rejection, and non-blocking object-model construction
- Verify separate read-navigation and write-mutation capabilities
- Verify raw snapshot reads do not require a schema registry or persistence access
- Verify wire fields, REST paths, and explicit JPA entity names remain unchanged; database table
  names are covered by the subsequent `objs_*` namespace migration
- Run the affected unit and integration test suites without introducing generator code
- Record migration findings and any follow-up work before the codegen stages begin

## Stop / review gate

Do not start WI-003 until the user reviews and accepts the complete public rename, dependency
direction, runtime behavior, and test evidence. Any unexpected persistence, server, serialization,
or consumer behavior change stops the story at this gate.

## Out of scope

- JSON Schema exporter changes
- Generator implementation
- Generated application sources
- REST or persistence behavior changes

## Acceptance

- [x] All intended consumers compile against the unprefixed API
- [x] No old public `BoM` aliases or compatibility facades remain
- [x] No dependency cycle or framework dependency enters `objs-api`
- [x] Existing server, wire, REST, JPA, and storage behavior remains unchanged
- [x] Raw graph data is readable without schema-registry access
- [x] Read/write capability and Jackson boundaries are verified
- [x] Repository-wide migration checks and affected tests pass
- [x] The destructive stage is explicitly accepted before WI-003 begins

## Gate evidence

- Repository-wide source scan found no old root-library `BoM`/`Bom` type declarations, imports, or
  compatibility aliases. SBOM example domain types remain intentionally named for that domain.
- `objs-api` source contains no Spring, JPA, Hibernate, or persistence references.
- `objs-api` runtime dependencies are limited to Jackson, Kotlin stdlib, and transitive Jackson
  core/annotations; `objs-core` is the only root module with a direct API dependency.
- API tests verify caller-supplied Jackson hydration, raw reads without bindings, immutable
  navigation, UUID-only duplicate rejection, and independent read/write surfaces.
- `.\gradlew.bat test --no-configuration-cache` passed for the repository unit suite, including
  core, service, Gremlin, SBOM, asset-repository, and API consumers.
- Wire fields (`entities`, `edges`, `set`, `unset`), REST paths, and explicit JPA entity names were
  not changed by the class migration. Table naming is handled by Flyway V6, which moves Objs tables
  from `bom_*` to `objs_*`.

WI-003 remains gated until this complete rename and stabilization evidence is reviewed.
