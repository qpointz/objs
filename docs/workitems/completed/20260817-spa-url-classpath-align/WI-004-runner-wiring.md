# WI-004 — Workbench runner wiring

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — Runner  
**Status:** complete  
**Depends on:** WI-003

## Goal

Finish `:objs-service-app` packaging: the runner owns `:objs-service-ui`; `:objs-service` does not.

## Deliverables

- [x] Drop example denylist from `:objs-service-app`
- [x] `runtimeOnly(:objs-service-ui)` on `:objs-service-app`
- [x] Remove `:objs-service-ui` from `:objs-service`
- [x] Asset-repository sidecar `runtimeOnly(:objs-service-ui)`
- [x] `useJUnitJupiter()` (no catalog version pin) on JVM test suites

## Acceptance

- `:objs-service` has no project dependency on `:objs-service-ui`
- Workbench SPA still on `:objs-service-app` and example sidecars
