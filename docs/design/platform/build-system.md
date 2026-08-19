# Build system

## Layout

```text
objs/
  settings.gradle.kts      # includes leaf modules + examples/*
  build.gradle.kts         # group, version, toolchain, aggregate test tasks
  libs.versions.toml       # Spring Boot, TinkerPop, JUnit, node-gradle, …
  VERSION                  # default project version (e.g. 0.1.0)
  gradle/wrapper/          # Gradle 9.4.0
  objs-core/
  objs-service/
  objs-service-ui/         # workbench SPA (Vite); node-gradle 7.0.2
  objs-gremlin-core/
  objs-gremlin-service/
  objs-service-app/        # workbench-only runner (:8081)
  examples/
    sbom/
      sbom-service/        # :sbom-service (product app; :8080; no objs-service)
      sbom-service-ui/     # :sbom-service-ui
```

## Conventions

- **Plugins per leaf module:** `java-library` (Kotlin libraries), `java` + `com.github.node-gradle.node`
  on UI modules (`:objs-service-ui`, `:sbom-service-ui`), `application` (`objs-service-app`, `sbom-service`); Kotlin `jvm` on Kotlin modules;
  `kotlin-spring` on Spring modules; `kotlin-jpa` on `:objs-core` only
- **BOM:** Gradle `platform(libs.boot.dependencies)` — no `io.spring.dependency-management`
- **Catalog:** `libs.versions.toml` is the version SoT; declare only deps/plugins in use
- **Toolchain:** Java 21 applied to all Java/Kotlin projects from the root `subprojects` block
- **Tests:** JVM Test Suite + JUnit Jupiter from the version catalog; `testIT` on `:objs-core`
- **Aggregate:** root tasks `test` and `testIT` depend on leaf module tasks
- **No** `build-logic` / custom convention plugins
- **CI:** GitLab child pipelines — [`ci-pipeline.md`](ci-pipeline.md). Invokes `test` / `testIT` only; no extra Gradle tasks.
- **Workbench UI:** `:objs-service-ui` downloads Node via node-gradle, runs `npm ci` /
  `npm run build`, writes Vite output to `build/generated/vite`, then `processResources` copies
  into `build/resources/main/static/workbench/` (so project dependency classpaths see the assets).
  `:objs-service-app` (and example sidecars) depend with `runtimeOnly`. Skip with `-PskipUi=true`.
  **Convention:** HTTP prefix = classpath folder under `static/` (`/workbench` → `static/workbench`,
  `/ar` → `static/ar`, `/sbom` → `static/sbom`).
- **SBOM UI:** same pattern for `:sbom-service-ui` → `:sbom-service` `runtimeOnly`.
- **SBOM compile lock:** `:sbom-service` must not compile against `:objs-service`, `:objs-service-ui`,
  `:objs-gremlin-service`, or `:objs-service-app` (enforced in `sbom-service/build.gradle.kts`).

## Useful commands

```bash
./gradlew test
./gradlew :objs-core:build
./gradlew :objs-service:build
./gradlew :objs-service-ui:build
./gradlew :objs-service:build -PskipUi=true
./gradlew :objs-core:testIT
./gradlew :objs-service-app:run  # workbench only → :8081
./gradlew :sbom-service:run      # SBOM inventory → :8080
```
