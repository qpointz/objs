# WI-003 — Docs + verify

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Docs + verify  
**Status:** done  
**Depends on:** WI-002

## Goal

Document the new module/plugin build path and smoke-verify packaged + skip paths.

## Delivered

- [`AGENTS.md`](../../../../AGENTS.md), [`build-system.md`](../../../design/platform/build-system.md), [`ui.md`](../../../design/ui.md), [`service/README.md`](../../../design/service/README.md), [`sbom/example.md`](../../../design/sbom/example.md)
- Verified:
  - `./gradlew :objs-service-ui:build` — SUCCESS (Node 22.14.0 download + vite)
  - `./gradlew :objs-service-ui:build -PskipUi=true` — SUCCESS
  - `./gradlew :objs-service:build` / `-PskipUi=true` — SUCCESS
  - UI JAR contains `static/ui/index.html`

## Acceptance

- [x] Docs/AGENTS reflect `:objs-service-ui` + node-gradle 7.0.2  
- [x] Verify commands above succeed
