# WI-002 — Codegen foundation-dir path guard

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Codegen guard  
**Status:** planned  
**Depends on:** WI-000 (independent of WI-001)  
**Examples:** **SBOM / AR / codegen examples** (any app that runs `JavaCodeGenerator`)

## Goal

Replace `startsWith("objs-")` in `validateOutputDirectory` with an exact set of foundation
module directory names so nested monorepos and incidental / app `objs-*` path segments are allowed.

## Deliverables

- [ ] Exact foundation module dir set in [`JavaCodeGenerator.kt`](../../../../objs-codegen-java/src/main/kotlin/org/poc/objs/codegen/java/JavaCodeGenerator.kt)
- [ ] Tests: reject `…/a/b/z/objs-api/…`; allow `objs-my-monorepo/…` and `objs-sbom-service/…`
- [ ] README wording in [`objs-codegen-java/README.md`](../../../../objs-codegen-java/README.md)
- [ ] Ship G-2 locked decision (exact foundation dir set)

## Acceptance

- [ ] `./gradlew :objs-codegen-java:test`
- [ ] Message still indicates foundation-module ownership when rejecting

## Out of scope

- Expanding export module move list (G-3 deferred)
