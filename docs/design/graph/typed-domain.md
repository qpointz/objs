# Typed domain layer

**Status:** implemented (story [`sbom-typed-example`](../../workitems/completed/20260728-sbom-typed-example/STORY.md))

Reusable helpers in `objs-core` (`org.poc.objs.core.typed`) convert typed domain objects to foundation
`BoMEntity` / `BoMEdge` / `BoMGraph` **by composition** (not subclassing).

## APIs

| Type | Role |
|------|------|
| `EntityTypeMeta` | `(type, schemaVersion)` catalog key |
| `TypedEntity<P>` | Typed payload + annotations → `toBoMEntity()` |
| `TypedEdge` / `TypedEdgeMeta` | Role + optional property schema → `toBoMEdge` |
| `GraphBuilder` | Provisional UUIDs, local keys, default annotations → `BoMGraph` |
| `RegistryPack` | Register authoritative DSL schemas + allow-list rules; `objectSchema` helper |
| `PayloadMapper` | Shared JsonMapper (Kotlin module); nulls stripped from maps |

Schema authors use `BoMSchemaDsl` helpers or construct the serializable DSL directly. See
[object-schema-dsl.md](object-schema-dsl.md).

First consumer: [`../sbom/example.md`](../sbom/example.md).
