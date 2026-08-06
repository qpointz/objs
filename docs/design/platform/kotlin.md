# Kotlin implementation notes

**Status:** foundation story implemented in **Kotlin** (not Java).

| Item | Choice |
|------|--------|
| Language | Kotlin 2.2.x |
| JVM toolchain | **21** |
| Plugins | `kotlin.jvm`, `kotlin.spring`, `kotlin.jpa` |
| JSON Schema | `com.networknt:json-schema-validator` (Jackson 2 tree via shared ObjectMapper) |
| UUID | `java.util.UUID.randomUUID()` |

Domain docs under [`graph/`](graph/README.md); module map under [`core/README.md`](core/README.md).
