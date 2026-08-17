# Gaps — spa-url-classpath-align

| ID | Topic | Status | Resolution |
|----|-------|--------|------------|
| G-1 | Classpath vs URL mismatch | **resolved** | HTTP prefix = `static/<same-name>/`: workbench, `/ar`, `/sbom` |
| G-2 | `SpaRoutingFilter` blank-path default `/app` | **resolved** | Blank fallback is `/workbench` |
| G-3 | Old `/app` and `/ui` bookmarks | **wontfix** | No compatibility redirects (plan lock) |
| G-4 | `objs-app` name after examples exist | **resolved** | Renamed `:objs-service-app`; workbench-only; no example deps |
| G-5 | `:objs-service` pulled the workbench SPA | **resolved** | UI is `runtimeOnly` of `:objs-service-app` and example sidecars only |
