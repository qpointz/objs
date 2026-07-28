# WI-001 — Module shell `objs-sbom-example`

**Story:** [`STORY.md`](STORY.md)  
**Status:** done  
**Depends on:** —  
**Gaps:** G-S1, G-S30

## Goal

Add Gradle leaf module `objs-sbom-example` that depends on `:objs-core`, ready for SBOM domain code.

## Scope

- `settings.gradle.kts`: `include(":objs-sbom-example")`
- Module `build.gradle.kts` aligned with other leaves (Kotlin, JUnit, depend on `:objs-core`)
- Package root `org.poc.objs.sbom` (placeholder source or package marker)
- Do **not** add web/REST deps yet (WI-004 / G-S30); do **not** wire into `objs-app` yet (WI-006)

## Out of scope

- Typed toolkit (WI-002)
- Component / registry pack (WI-003)

## Acceptance

- [x] `:objs-sbom-example` compiles in the multi-module build
- [x] Module appears in Gradle project list; no SBOM domain types yet (or only package marker)
