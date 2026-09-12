# Programmatic graph recipes

Copy-paste Kotlin patterns for Boot apps that embed objs via `:objs-autoconfigure`.
Companion to the write-path sketch ([persist-sketch.md](persist-sketch.md)) — recipes show **calls**, the sketch explains **mechanics**.

**Inject stores, not DAOs.** `NamedGraphStore` / `GraphStore` own the unit of work. `*Dao` beans exist for extension only.

## Spring wiring

```kotlin
// build.gradle.kts
implementation(project(":objs-autoconfigure"))
// optional REST surface:
implementation(project(":objs-service"))
```

```kotlin
@Service
class MyGraphService(
    private val namedGraphs: NamedGraphStore,
    private val graphStore: GraphStore,
) { /* … */ }
```

`ObjsCoreAutoConfiguration` registers catalogs, validator, versioning (`ExplicitOnly` by default),
`NamedGraphStore`, `GraphStore`, and a no-op `GraphVersionReferenceGuard`. Apps that reference graph
versions externally (e.g. SBOM fingerprints) should provide a `GraphVersionReferenceGuard` bean so
purge/destroy fail closed.

## Create / update / delete

```kotlin
// Create empty header (optional seed membership by existing pool ids)
val g = namedGraphs.create(
    GraphSpec(annotations = mapOf("kind" to "demo"), entityIds = emptySet()),
)

// MERGE patch (Save in Composer)
namedGraphs.mutate(
    g.id,
    graphMutation {
        mode(MutationMode.MERGE)
        entities { set(/* Entity… */) }
        edges { set(/* Edge… */) }
    },
)

// REPLACE overwrite (Overwrite… in Composer)
namedGraphs.mutate(
    g.id,
    graphMutation {
        mode(MutationMode.REPLACE)
        entities { set(/* full desired membership */) }
        edges { set(/* full desired edges */) }
    },
)

// Clear live HEAD members+edges; keep header + history
namedGraphs.clearGraph(g.id)

// Softish delete: drop live header; history remains readable
namedGraphs.delete(g.id)

// Full wipe: HEAD + all deep versions (blocked if GraphVersionReferenceGuard says in-use)
namedGraphs.destroyGraph(g.id)
```

REST: `POST /graphs/{id}/clear`, `DELETE /graphs/{id}` (softish), `DELETE /graphs/{id}/destroy`.

## Versions

```kotlin
// Freeze now (Option B clocks = Instant.now())
val v1 = namedGraphs.createDeepGraphVersion(g.id, mapOf("label" to "gate"))

// Backdated freeze for migration (version = max(atMillis, prev+1); new freeze rows share at)
val at = Instant.parse("2020-06-15T12:00:00Z")
val vPast = namedGraphs.createDeepGraphVersion(g.id, mapOf("era" to "2020"), at)

namedGraphs.listGraphVersions(g.id)
namedGraphs.getGraphVersion(g.id, vPast.version)

// Purge one freeze (rejects current head_version)
namedGraphs.purgeGraphVersion(g.id, v1.version)

// Purge all freezes; null head_version; live HEAD intact
namedGraphs.purgeAllGraphVersions(g.id)

// Explicit orphan GC after purge
namedGraphs.compactEntity(entityId)
namedGraphs.compactEdge(edgeId)
```

REST: `POST /graphs/{id}/versions` body `{ "annotations": {}, "createdAt": "…" }`,
`DELETE /graphs/{id}/versions/{v}`, `DELETE /graphs/{id}/versions`,
`POST /entities/{id}/compact`, `POST /edges/{id}/compact`.

Stage D (travel-back reset / membership-only apply):

```kotlin
namedGraphs.resetGraphToVersion(g.id, vPast.version)              // restore payloads + set head
namedGraphs.resetGraphToVersion(g.id, vPast.version, truncateAfter = true)
namedGraphs.applyGraphVersionMembership(g.id, vPast.version)      // structure only; head unchanged
```

REST: `POST /graphs/{id}/versions/{v}/reset?truncateAfter=`,
`POST /graphs/{id}/versions/{v}/apply-membership`.

## Annotations vs catalog tags

| Concept | Where | Purpose |
|---------|--------|---------|
| **Annotations** | Graph / entity / edge maps | Free-form `Map<String,String>` for matchers, product metadata, UI |
| **Tags** | Schema catalog / allow-list only | Controlled vocabulary on types — **not** instance annotation keys |

Do not treat annotation keys as schema tags. Matcher `obj-expr` / `graph-expr` read annotations;
catalog tags are for ontology UX and export, not graph lifecycle.

## Related

- [persist-sketch.md](persist-sketch.md) — mutate order, REPLACE-empty ≡ `clearGraph`
- [persistence.md](persistence.md) — tables, clocks, Flyway
- [rest-api.md](../service/rest-api.md) — HTTP glossary
- [apps-vs-foundation.md](apps-vs-foundation.md) — store vs example apps
- [spring-split.md](../core/spring-split.md) — module boundaries
