# Unit testing sketch — `ResolvedGraph` and multi-version schemas

How consumer apps unit-test logic built on objs read models without a database or Spring context.
Covers two cases:

1. Code that consumes [`ResolvedGraph`](../../../objs-api/src/main/kotlin/org/poc/objs/api/domain/GraphModels.kt)
2. Code that needs several schema versions of one type and uses schema evolution (L2 payload upgrade, see [`schema-evolution.md`](schema-evolution.md))

**Rule of thumb:** build real values, mock only ports. `ResolvedGraph`, `GraphContents`, `Entity`,
`Edge`, `Schema` are plain data classes; catalogs and upgrade registries have in-memory
implementations in `objs-api`. Mock the store (`NamedGraphStore`, `DeepGraphVersionService`)
that *returns* those values, or better, keep domain logic store-free.

---

## 1. `ResolvedGraph`

### Shape

```kotlin
data class ResolvedGraph(
    val id: UUID,
    val annotations: Map<String, String>,
    val contents: GraphContents,          // entities + edges (GraphFragment)
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)
```

No behaviour, no dependencies → **do not mock it**. Kotlin classes are final; mocking would need the
inline mock maker for zero benefit and couples tests to accessors instead of data.

### Test fixture

```kotlin
object TestGraphs {
    fun entity(
        type: String = "Component",
        schemaVersion: String = "1.0.0",
        vararg payload: Pair<String, Any?>,
    ) = Entity(
        id = UUID.randomUUID(),
        type = type,
        schemaVersion = schemaVersion,
        payload = mutableMapOf(*payload),
    )

    fun edge(from: Entity, to: Entity, role: String = "depends_on") =
        Edge(id = UUID.randomUUID(), source = from.id!!, target = to.id!!, role = role)

    fun resolvedGraph(
        entities: List<Entity> = emptyList(),
        edges: List<Edge> = emptyList(),
        annotations: Map<String, String> = emptyMap(),
        id: UUID = UUID.randomUUID(),
    ) = ResolvedGraph(id, annotations, GraphContents(entities, edges))
}
```

### Pure domain logic (preferred)

Take `ResolvedGraph` (or `GraphFragment` when only entities/edges matter) as a parameter —
same pattern as SBOM `BomUnion.of(graphs: List<ResolvedGraph>)`.

```kotlin
class DependencyCounterTest {
    @Test
    fun shouldCountOutgoingEdges_whenComponentHasDependencies() {
        val a = TestGraphs.entity("Component", "1.0.0", "name" to "a")
        val b = TestGraphs.entity("Component", "1.0.0", "name" to "b")
        val c = TestGraphs.entity("Component", "1.0.0", "name" to "c")
        val graph = TestGraphs.resolvedGraph(
            entities = listOf(a, b, c),
            edges = listOf(TestGraphs.edge(a, b), TestGraphs.edge(a, c)),
        )

        assertThat(DependencyCounter.outgoing(graph, a.id!!)).isEqualTo(2)
    }
}
```

### Service that loads the graph (mock the port)

```kotlin
class MyGraphServiceTest {
    private val store = mock<NamedGraphStore>()
    private val service = MyGraphService(store)

    @Test
    fun shouldReturnSummary_whenGraphExists() {
        val a = TestGraphs.entity()
        val b = TestGraphs.entity()
        val graphId = UUID.randomUUID()
        whenever(store.get(graphId)).thenReturn(
            TestGraphs.resolvedGraph(listOf(a, b), listOf(TestGraphs.edge(a, b)), id = graphId),
        )

        val summary = service.summary(graphId)

        assertThat(summary.entityCount).isEqualTo(2)
    }

    @Test
    fun shouldFail_whenGraphMissing() {
        whenever(store.get(any())).thenReturn(null)
        assertThatThrownBy { service.summary(UUID.randomUUID()) }
            .isInstanceOf(GraphException::class.java)
    }
}
```

`NamedGraphStore` is a final Kotlin class: Mockito 5 (inline mock maker by default) or MockK.
Other `ResolvedGraph` sources: `NamedGraphStore.create / replace / clone / copyGraph`,
`DeepGraphVersionService.getGraphVersion(graphId, version)`.

---

## 2. Multiple schema versions + schema evolution

### What depends on what

| Concern | Type | Test implementation |
|---------|------|---------------------|
| Schema definitions per `(type, version)` | `SchemaCatalog` | `InMemorySchemaCatalog` |
| Latest version / field hints | `CatalogSupport` | `CatalogSupport(catalog, InMemoryAllowedEdgeCatalog())` |
| Upgrade steps | `SchemaUpgradeRegistry` | `InMemorySchemaUpgradeRegistry.of(...)` |
| Latest pin + payload class | `SchemaUpgradeTargetRegistry` | lambda (`fun interface`) |
| Exact-pin bindings | `TypedEntityBindingRegistry` | lambda; `{ _, _ -> null }` forces upgrade path |
| Map ↔ class | `PayloadMapper` | wraps Jackson `JsonMapper` |

L2 runtime (`PayloadUpgrade`, `TypedGraphView`) does **not** read `SchemaCatalog`. The catalog is
only needed when your code uses it (latest lookup, field hints, validation). The sketch below
derives upgrade targets from the catalog so both stay consistent.

### Schema fixture (v1 + v2 of `Product`)

```kotlin
object ProductSchemas {
    val v1 = RegistryPack.objectSchema(
        type = "Product", version = "1.0.0",
        fields = listOf(
            SchemaDsl.field("name", SchemaDsl.string("Name", "Product name"), identifier = true),
        ),
    )

    val v2 = RegistryPack.objectSchema(
        type = "Product", version = "2.0.0",
        fields = listOf(
            SchemaDsl.field("displayName", SchemaDsl.string("Display name", "Product name"), identifier = true),
            SchemaDsl.field("tier", SchemaDsl.string("Tier", "Service tier"), required = false),
        ),
    )

    fun catalog(vararg schemas: Schema = arrayOf(v1, v2)): SchemaCatalog =
        InMemorySchemaCatalog().apply { schemas.forEach(::register) }
}
```

- `register` runs `SchemaNormalizer.normalizeStrict` — every node needs title + description.
- With edge rules: `RegistryPack(schemas = ..., edgeRules = ...).registerInto(schemaCatalog, edgeCatalog)`.

### Payload classes and step

```kotlin
data class ProductV1(var name: String? = null)                          // Lane B snapshot shape
data class ProductV2(var displayName: String? = null, var tier: String? = null) // Lane A latest

class Product_1_0_0_to_2_0_0 : ClassToClassUpgradeStep<ProductV1, ProductV2>() {
    override fun type() = "Product"
    override fun fromVersion() = "1.0.0"
    override fun toVersion() = "2.0.0"
    override fun fromClass() = ProductV1::class.java
    override fun toClass() = ProductV2::class.java
    override fun upgrade(from: ProductV1) = ProductV2(displayName = from.name, tier = "STANDARD")
}
```

Additive-only changes can use `MapToClassUpgradeStep<T>` or no step at all (additive fallback).

### Upgrade fixture

```kotlin
class UpgradeFixture(
    catalog: SchemaCatalog = ProductSchemas.catalog(),
    steps: List<SchemaUpgradeStep> = listOf(Product_1_0_0_to_2_0_0()),
    private val latestClasses: Map<String, Class<*>> = mapOf("Product" to ProductV2::class.java),
) {
    val mapper = PayloadMapper(
        JsonMapper.builder()
            .addModule(kotlinModule())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build(),
    )
    val support = CatalogSupport(catalog, InMemoryAllowedEdgeCatalog())
    val upgrades: SchemaUpgradeRegistry = InMemorySchemaUpgradeRegistry.of(steps)
    val targets = SchemaUpgradeTargetRegistry { type ->
        val latest = support.latestEntitySchema(type) ?: return@SchemaUpgradeTargetRegistry null
        latestClasses[type]?.let { SchemaUpgradeTarget(latest.version, it) }
    }
    val noBindings = TypedEntityBindingRegistry { _, _ -> null }

    fun hydrate(entity: Entity): PayloadUpgrade.Outcome =
        PayloadUpgrade.hydrate(entity, noBindings, mapper, HydrationPolicy.UPGRADE_TO_LATEST, upgrades, targets)

    fun view(fragment: GraphFragment): TypedGraphView =
        TypedGraphView.from(fragment, noBindings, mapper, HydrationPolicy.UPGRADE_TO_LATEST, upgrades, targets)
}
```

Adding a `v3` schema to the catalog moves the target automatically (keep `latestClasses` and
steps in sync). Static alternative:
`SchemaUpgradeTargetRegistry { if (it == "Product") SchemaUpgradeTarget("2.0.0", ProductV2::class.java) else null }`.

### Tests

```kotlin
class ProductUpgradeTest {
    private val fx = UpgradeFixture()

    @Test
    fun shouldUpgradeToLatest_whenStepRegistered() {
        val entity = TestGraphs.entity("Product", "1.0.0", "name" to "Widget")

        val outcome = fx.hydrate(entity)

        val latest = outcome.hydrated as ProductV2
        assertThat(latest.displayName).isEqualTo("Widget")
        assertThat(latest.tier).isEqualTo("STANDARD")
        assertThat(outcome.diagnostics!!.effectiveSchemaVersion).isEqualTo("2.0.0")
        assertThat(outcome.diagnostics!!.stepsApplied).containsExactly("1.0.0->2.0.0")
    }

    @Test
    fun shouldFailOpenToRaw_whenRenameHasNoStep() {
        val fx = UpgradeFixture(steps = emptyList())
        val entity = TestGraphs.entity("Product", "1.0.0", "name" to "Widget")

        val outcome = fx.hydrate(entity)

        assertThat(outcome.hydrated).isNull()
        assertThat(outcome.diagnostics!!.additiveFallback).isTrue()
        assertThat(outcome.diagnostics!!.failure).isNotBlank()
    }

    @Test
    fun shouldProjectWholeGraph_whenResolvedGraphHasMixedPins() {
        val old = TestGraphs.entity("Product", "1.0.0", "name" to "Old")
        val graph = TestGraphs.resolvedGraph(entities = listOf(old))

        val node = fx.view(graph.contents).node(old.id!!)!!

        assertThat(node.schemaVersion).isEqualTo("1.0.0")            // evidence (as stored)
        assertThat(node.effectiveSchemaVersion).isEqualTo("2.0.0")   // examine (latest)
        assertThat((node.hydratedPayload as ProductV2).displayName).isEqualTo("Old")
    }
}
```

### Scenarios worth covering

| Scenario | Setup | Expect |
|----------|-------|--------|
| Exact latest pin | binding for `(Product, 2.0.0)` | `UpgradeDiagnostics.exact`, no steps |
| Chain | steps `1.0.0→1.1.0`, `1.1.0→2.0.0` | `findChain` returns 2 steps, applied in order |
| Ambiguous | two steps `1.0.0→2.0.0` | `findChain` throws `AmbiguousUpgradePathException`; `hydrate` reports it in `failure` |
| Additive bump, no step | v1.1 adds optional field only | hydrated, `additiveFallback = true` |
| Rename, no step | v2 renames field | `hydrated = null`, `failure` set (fail-open raw) |
| Unknown type | no target for type | `failure = "no latest upgrade target for type"` |

### Combined: service over store + upgrades

```kotlin
val store = mock<NamedGraphStore>()
whenever(store.get(graphId)).thenReturn(TestGraphs.resolvedGraph(listOf(oldProduct)))

val service = ProductExamineService(store, fx.mapper, fx.upgrades, fx.targets)
val view = service.latest(graphId)   // read projection only — never write upgraded payloads back
```

---

## References

- `objs-api/src/test/kotlin/org/poc/objs/api/typed/upgrade/SchemaUpgradeTest.kt`
- `examples/sbom/sbom-service/src/test/kotlin/org/poc/objs/sbom/migration/ComponentUpgradeRegressionTest.kt`
- `examples/sbom/sbom-service/src/test/kotlin/org/poc/objs/sbom/domain/BomUnionTest.kt`
- [`schema-evolution.md`](schema-evolution.md), [`object-schema-dsl.md`](object-schema-dsl.md), [`typed-conversion-recipes.md`](typed-conversion-recipes.md)
