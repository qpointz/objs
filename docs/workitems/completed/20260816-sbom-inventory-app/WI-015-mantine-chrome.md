# WI-015 — Mantine chrome + workbench sidecar

**Story:** [`STORY.md`](STORY.md)  
**Status:** complete  

## Goal

Match asset-repository look and feel (Mantine AppShell, pill nav, dark/light) while keeping
Applications / Portfolios journeys. Same-origin Workbench link when foundation jars are present.
Refresh/deep-link `/ui/**` via a local SPA routing filter.

## Deliverables

- [x] Mantine + Tabler in `:sbom-service-ui`; `AppLayout` chrome  
- [x] Inventory SPA stays at `/ui/`; classpath pack `static/sbom` (not workbench `static/ui`)  
- [x] `SbomSpaRoutingFilter` (copy of foundation SPA filter logic) for `/ui`  
- [x] `runtimeOnly` `:objs-service` / `:objs-service-ui` / `:objs-gremlin-service`; Gradle still forbids compile coupling  
- [x] Remove `LegacyWorkbenchUiRedirectController` so `/ui` is free for the inventory SPA  
- [x] Header Workbench probe; hidden when `/workbench/` is absent  

## Acceptance

- [x] Inventory UI matches AR chrome tokens without collection-grid IA  
- [x] `GET /ui/applications/{id}` serves the SPA index  
- [x] Workbench at `/workbench/` when runtime jars present; inventory still compiles without `implementation` of objs-service  
