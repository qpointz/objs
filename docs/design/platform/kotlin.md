# Kotlin implementation notes

**Status:** foundation story implemented in **Kotlin** (not Java).

| Item | Choice |
|------|--------|
| Language | Kotlin 2.2.x |
| JVM toolchain | **24** (Kotlin does not yet target JDK 25; Java toolchain aligned to 24) |
| Plugins | `kotlin.jvm`, `kotlin.spring`, `kotlin.jpa` |
| JSON Schema | `com.networknt:json-schema-validator` (Jackson 2 tree via shared ObjectMapper) |
| UUID | `java.util.UUID.randomUUID()` |

Domain docs under [`graph/`](graph/README.md); module map under [`core/README.md`](core/README.md).
