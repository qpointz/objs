# Build system

## Layout

```text
objs/
  settings.gradle.kts      # includes :objs-core, :objs-service
  build.gradle.kts         # group, version, toolchain, aggregate test tasks
  libs.versions.toml       # Spring Boot, JUnit, Lombok, Jackson, …
  VERSION                  # default project version (e.g. 0.1.0)
  gradle/wrapper/          # Gradle 9.4.0
  objs-core/
  objs-service/
```

## Conventions

- **Plugins per leaf module:** `java-library`, `io.spring.dependency-management`
- **BOM:** `spring-boot-dependencies` imported in each Spring module
- **Toolchain:** Java 25 applied to all Java projects from the root `subprojects` block
- **Tests:** JVM Test Suite + JUnit Jupiter from the version catalog; optional `testIT` suite on
  `objs-service`
- **Aggregate:** root tasks `test` and `testIT` depend on leaf module tasks
- **No** `build-logic` / custom convention plugins

## Useful commands

```bash
./gradlew test
./gradlew :objs-core:build
./gradlew :objs-service:build
./gradlew :objs-service:testIT
```
