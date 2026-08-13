# Gaps — ui-gradle-node

| ID | Topic | Status | Resolution |
|----|-------|--------|------------|
| G-1 | Node LTS pin for `node { version }` | **resolved** | `22.14.0` (download via node-gradle) |
| G-2 | Centralized repos vs node download Ivy | **resolved** | Node download works via node-gradle project repos; use `java` (not `java-library`) so root Jacoco is not applied (avoids repo shadow) |
| G-3 | `api` vs `implementation` / `runtimeOnly` on `:objs-service` → UI JAR | **resolved** | `runtimeOnly(project(":objs-service-ui"))` |
| G-4 | Module path | **resolved** | Top-level `objs-service-ui/` (moved from `objs-service/ui/`) |
| G-5 | Avoid Sync | **resolved** | Vite → `build/generated/vite`; `processResources` → `static/ui/` (needed so project runtime classpath sees assets; jar `from` alone is not enough) |
| G-6 | `/workbench` 404 after module split | **resolved** | SPA was only in jar `from{}`; Gradle project deps expose resources dirs — fixed via `processResources` |
