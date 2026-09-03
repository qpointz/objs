# WI-005 — Consumer Gradle

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 5 — Consumers  
**Status:** done — [`3886329`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/3886329)  
**Depends on:** WI-004 (done)  
**Examples:** **SBOM** (`:sbom-service` dependency swap)

## Goal

Route Boot consumers through `:objs-autoconfigure`; keep graph frontends on `:objs-api` only.

## Scope

- [x] [`objs-service/build.gradle.kts`](../../../../objs-service/build.gradle.kts) — `api(project(":objs-autoconfigure"))`
- [x] [`examples/sbom/sbom-service/build.gradle.kts`](../../../../examples/sbom/sbom-service/build.gradle.kts) — `implementation(project(":objs-autoconfigure"))` instead of `:objs-core`
- [x] Root [`build.gradle.kts`](../../../../build.gradle.kts) CI `test` task includes `:objs-api:test` and `:objs-autoconfigure:test`
- [x] Verify `:objs-gremlin-core` still `api(:objs-api)` only — no Boot / core leakage
- [x] Asset-repository Java sources retargeted `org.poc.objs.core.{domain,match,seed,validation}` → `org.poc.objs.api.*` (compile)

## Out of scope

- Export script (WI-007)
- Full test suite green (WI-006)

## Acceptance

- `./gradlew :objs-service:compileKotlin :sbom-service:compileKotlin :objs-gremlin-core:compileKotlin`
