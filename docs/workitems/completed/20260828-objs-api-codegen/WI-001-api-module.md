# WI-001 — `objs-api` module scaffold and dependency boundary

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 1 — API boundary
**Status:** completed
**Depends on:** WI-000
**Implementation commit:** [`0962f76`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/0962f76)

## Goal

Create the standalone Kotlin/JVM `:objs-api` library that application-generated code can consume
without bringing in Spring or persistence infrastructure. The module is schema-agnostic and owns
no generated application classes.

This stage creates only the boundary. Existing runtime classes and public imports remain in place
until the separate destructive extraction stage.

## Scope

- Add `:objs-api` to `settings.gradle.kts`
- Add Kotlin/JVM and `java-library` configuration with Java 21 alignment
- Establish `org.poc.objs.api.*` package layout
- Keep the public runtime independent of application schemas, ontology classes, and generated
  catalogs
- Add only the dependencies approved by WI-000, with Jackson as the supported payload codec
- Do not create or configure a process-wide `ObjectMapper`; expose constructors or codec
  parameters so consuming applications supply their configured mapper
- Add Java and Kotlin compile/test fixtures
- Add dependency verification proving no Spring, Boot, JPA, servlet, database, or validator artifacts
- Record a stop point before any existing runtime class is moved or renamed

## Out of scope

- Moving existing runtime classes
- Relation manifest or exporter changes
- Second generator
- REST clients or persistence

## Acceptance

- [x] `./gradlew :objs-api:build` succeeds
- [x] A Kotlin consumer compiles against the module
- [x] A Java consumer compiles against the module
- [x] Dependency report contains no Spring/JPA/Boot/persistence/validator dependency
- [x] No schema-specific generated source or application ontology is present in the module
- [x] Module package and public API naming follow WI-000
- [x] Existing runtime behavior and public class locations remain unchanged by this WI
- [x] The stage is explicitly reviewable before WI-002 begins

