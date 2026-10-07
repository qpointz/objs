# Schema upgrade to latest — Java sketch

**Status:** sketch (not compiled; verify against current API before copying)  
**Audience:** consumer apps written in Java on top of objs  
**Related:** [schema-evolution.md](schema-evolution.md) (L2 design), [unit-testing-sketch.md](unit-testing-sketch.md) (Kotlin fixtures), [seeds.md](seeds.md)

Pattern: graphs are **persisted with the pin they were written with** (pin-and-keep); the **wire
always carries the latest** schema version. One read-side service upgrades a `ResolvedGraph` into a
new `ResolvedGraph`; stored data is never rewritten on read.

```text
HTTP (latest only) ─► Controller ─► LatestGraphService ─► GraphUpgrader ─► PayloadUpgrade (objs-api)
                                         │
                                  NamedGraphStore (pin-and-keep)
```

Contents:

1. [Upgrade `ResolvedGraph` → `ResolvedGraph`](#1-upgrade-resolvedgraph--resolvedgraph)
2. [Mock schema evolution in unit tests](#2-mock-schema-evolution-in-unit-tests)
3. [Seed-backed upgrade rig](#3-seed-backed-upgrade-rig)
4. [Spring wiring](#4-spring-wiring)

### Kotlin → Java interop notes

| Kotlin API | From Java |
|------------|-----------|
| `PayloadUpgrade.hydrate(...)` (`@JvmStatic`) | `PayloadUpgrade.hydrate(...)`; result `PayloadUpgrade.Outcome` |
| `InMemorySchemaUpgradeRegistry.of(...)` (`@JvmStatic`) | `of(step1, step2)` or `of(collection)` |
| `fun interface` (`SchemaUpgradeTargetRegistry`, `TypedEntityBindingRegistry`, `SchemaUpgradeRegistry`) | Java lambda / method reference |
| `Entity`, `Edge`, `ResolvedGraph` (`@JvmOverloads`) | Constructors with trailing defaults omitted; no `copy` → use full constructor |
| `object SeedYaml` | `SeedYaml.INSTANCE.parseDocuments(...)` |
| Top-level `SEED_KIND_*` constants | `SeedModelsKt.SEED_KIND_GRAPH` |
| Kotlin default params without `@JvmOverloads` (e.g. `SeedImporter.importYaml(yaml, allowedKinds)`) | Pass every argument (`null` for nullable defaults) |
| `val isValid` | `isValid()` |

---

## 1. Upgrade `ResolvedGraph` → `ResolvedGraph`

### Payload classes and an upgrade step

Lane A latest class (`ProductV2`) plus a historic snapshot shape (`ProductV1`) used only by the step.

```java
public record ProductV1(String name) {}

public record ProductV2(String displayName, String tier) {}
```

```java
public final class Product_1_0_0_to_2_0_0 extends ClassToClassUpgradeStep<ProductV1, ProductV2> {
    @Override public String type() { return "Product"; }
    @Override public String fromVersion() { return "1.0.0"; }
    @Override public String toVersion() { return "2.0.0"; }
    @Override public Class<ProductV1> fromClass() { return ProductV1.class; }
    @Override public Class<ProductV2> toClass() { return ProductV2.class; }

    @Override
    public ProductV2 upgrade(ProductV1 from) {
        return new ProductV2(from.name(), "STANDARD");
    }
}
```

Additive-only bumps (new optional fields, no renames) need no step: `PayloadUpgrade` falls back to
strict Map → latest class.

### `GraphUpgrader`

Plain Java, no Spring. Edges and graph header pass through; input graph is not mutated.

```java
public final class GraphUpgrader {

    public enum FailurePolicy { FAIL, KEEP_STORED }

    public record Result(
            ResolvedGraph graph,
            Map<UUID, UpgradeDiagnostics> upgraded,
            Map<UUID, UpgradeDiagnostics> failed) {}

    private static final TypedEntityBindingRegistry NO_BINDINGS = (type, version) -> null;

    private final PayloadMapper mapper;
    private final SchemaUpgradeRegistry upgrades;
    private final SchemaUpgradeTargetRegistry targets;
    private final FailurePolicy onFailure;

    public GraphUpgrader(PayloadMapper mapper, SchemaUpgradeRegistry upgrades, SchemaUpgradeTargetRegistry targets) {
        this(mapper, upgrades, targets, FailurePolicy.FAIL);
    }

    public GraphUpgrader(PayloadMapper mapper, SchemaUpgradeRegistry upgrades,
                         SchemaUpgradeTargetRegistry targets, FailurePolicy onFailure) {
        this.mapper = mapper;
        this.upgrades = upgrades;
        this.targets = targets;
        this.onFailure = onFailure;
    }

    public ResolvedGraph upgrade(ResolvedGraph graph) {
        return upgradeWithReport(graph).graph();
    }

    public Result upgradeWithReport(ResolvedGraph graph) {
        Map<UUID, UpgradeDiagnostics> upgraded = new LinkedHashMap<>();
        Map<UUID, UpgradeDiagnostics> failed = new LinkedHashMap<>();
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : graph.getContents().getEntities()) {
            entities.add(upgradeEntity(graph.getId(), entity, upgraded, failed));
        }
        ResolvedGraph out = new ResolvedGraph(
                graph.getId(),
                graph.getAnnotations(),
                new GraphContents(entities, graph.getContents().getEdges()),
                graph.getCreatedAt(),
                graph.getUpdatedAt());
        return new Result(out, upgraded, failed);
    }

    private Entity upgradeEntity(UUID graphId, Entity e,
                                 Map<UUID, UpgradeDiagnostics> upgraded,
                                 Map<UUID, UpgradeDiagnostics> failed) {
        SchemaUpgradeTarget target = targets.latest(e.getType());
        if (target == null || e.getSchemaVersion().equals(target.getSchemaVersion())) {
            return e;
        }
        PayloadUpgrade.Outcome outcome = PayloadUpgrade.hydrate(
                e, NO_BINDINGS, mapper, HydrationPolicy.UPGRADE_TO_LATEST, upgrades, targets);
        UpgradeDiagnostics diagnostics = outcome.getDiagnostics();
        Object hydrated = outcome.getHydrated();
        if (hydrated == null) {
            failed.put(e.getId(), diagnostics);
            if (onFailure == FailurePolicy.FAIL) {
                throw new GraphUpgradeException(graphId, e, diagnostics);
            }
            return e;
        }
        upgraded.put(e.getId(), diagnostics);
        return new Entity(
                e.getId(),
                e.getType(),
                diagnostics.getEffectiveSchemaVersion(),
                mapper.toMap(hydrated),
                new LinkedHashMap<>(e.getAnnotations()),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getHeadVersion());
    }
}
```

```java
public class GraphUpgradeException extends IllegalStateException {
    private final UUID graphId;
    private final Entity entity;
    private final UpgradeDiagnostics diagnostics;

    public GraphUpgradeException(UUID graphId, Entity entity, UpgradeDiagnostics diagnostics) {
        super("Cannot upgrade %s@%s (%s) in graph %s: %s".formatted(
                entity.getType(), entity.getSchemaVersion(), entity.getId(), graphId,
                diagnostics == null ? "unknown" : diagnostics.getFailure()));
        this.graphId = graphId;
        this.entity = entity;
        this.diagnostics = diagnostics;
    }

    public UUID getGraphId() { return graphId; }
    public Entity getEntity() { return entity; }
    public UpgradeDiagnostics getDiagnostics() { return diagnostics; }
}
```

### Decisions encoded

| Case | Behaviour | Why |
|------|-----------|-----|
| Already at latest | returned as-is | `PayloadUpgrade` without bindings would report "no exact binding for latest pin" |
| No target for type | returned as-is | type not under app version control; throw instead if every type must be covered |
| Upgrade fails, `FAIL` (default) | `GraphUpgradeException` | returning stored payload under a latest contract silently breaks clients |
| Upgrade fails, `KEEP_STORED` | stored entity, listed in `Result.failed` | admin / diagnostics views |
| Edges | unchanged | L2 covers entity payloads only |
| Persistence | none | read projection only; never write upgraded payloads back on GET |

---

## 2. Mock schema evolution in unit tests

No mocks needed for catalogs or registries: use in-memory implementations from `objs-api` and the
real seed handlers from `objs-persistence` (`testImplementation(project(":objs-persistence"))`).
Schemas are authored as seed YAML so tests and deployment use the same format.

### Fixtures

```java
final class TestGraphs {
    private TestGraphs() {}

    static Entity entity(String type, String version, Map<String, Object> payload) {
        return new Entity(UUID.randomUUID(), type, version, new LinkedHashMap<>(payload));
    }

    static Edge edge(Entity from, Entity to, String role) {
        return new Edge(UUID.randomUUID(), null, from.getId(), to.getId(), role);
    }

    static ResolvedGraph graph(List<Entity> entities, List<Edge> edges) {
        return new ResolvedGraph(UUID.randomUUID(), Map.of(), new GraphContents(entities, edges));
    }
}
```

```java
final class ProductSchemas {
    private ProductSchemas() {}

    static final String YAML = """
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            type: Product
            version: "1.0.0"
            contentSchema:
              type: OBJECT
              title: Product
              description: Product v1
              fields:
                - name: name
                  identifier: true
                  schema: { type: STRING, title: Name, description: Product name }
            ---
            apiVersion: objs.poc.org/v1
            kind: ObjectSchema
            type: Product
            version: "2.0.0"
            contentSchema:
              type: OBJECT
              title: Product
              description: Product v2
              fields:
                - name: displayName
                  identifier: true
                  schema: { type: STRING, title: Display name, description: Product name }
                - name: tier
                  required: false
                  schema: { type: STRING, title: Tier, description: Service tier }
            """;
}
```

```java
final class UpgradeFixture {
    final InMemorySchemaCatalog schemas = new InMemorySchemaCatalog();
    final InMemoryAllowedEdgeCatalog rules = new InMemoryAllowedEdgeCatalog();
    final CatalogSupport support = new CatalogSupport(schemas, rules);
    final Validator validator = new Validator(schemas, rules);
    final PayloadMapper mapper = new PayloadMapper(
            JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build());
    final SchemaUpgradeRegistry upgrades;
    final SchemaUpgradeTargetRegistry targets;

    UpgradeFixture(List<String> seedYamls, List<SchemaUpgradeStep> steps, Map<String, Class<?>> latestClasses) {
        SeedImporter importer = new SeedImporter(
                List.of(new ObjectSchemaSeedHandler(schemas), new AllowedEdgeRuleSeedHandler(rules)),
                new PassthroughUnitOfWork());
        seedYamls.forEach(yaml -> importer.importYaml(yaml, null));   // throws SeedImportException on failure

        this.upgrades = InMemorySchemaUpgradeRegistry.of(steps);
        this.targets = type -> {
            Schema latest = support.latestEntitySchema(type);
            Class<?> cls = latestClasses.get(type);
            return latest == null || cls == null ? null : new SchemaUpgradeTarget(latest.getVersion(), cls);
        };
    }

    static UpgradeFixture products(SchemaUpgradeStep... steps) {
        return new UpgradeFixture(List.of(ProductSchemas.YAML), List.of(steps), Map.of("Product", ProductV2.class));
    }

    GraphUpgrader upgrader(GraphUpgrader.FailurePolicy policy) {
        return new GraphUpgrader(mapper, upgrades, targets, policy);
    }
}
```

Targets are derived from the catalog's latest version, so adding a `3.0.0` seed moves the target
automatically (keep `latestClasses` and steps in sync).

### `GraphUpgrader` tests

```java
class GraphUpgraderTest {

    @Test
    void shouldUpgradeOldPinsAndKeepLatest_whenGraphHasMixedVersions() {
        UpgradeFixture fx = UpgradeFixture.products(new Product_1_0_0_to_2_0_0());
        Entity old = TestGraphs.entity("Product", "1.0.0", Map.of("name", "Old"));
        Entity current = TestGraphs.entity("Product", "2.0.0", Map.of("displayName", "New", "tier", "GOLD"));
        ResolvedGraph graph = TestGraphs.graph(List.of(old, current), List.of(TestGraphs.edge(old, current, "replaces")));

        GraphUpgrader.Result result = fx.upgrader(GraphUpgrader.FailurePolicy.FAIL).upgradeWithReport(graph);

        Map<UUID, Entity> byId = result.graph().getContents().getEntities().stream()
                .collect(Collectors.toMap(Entity::getId, e -> e));
        assertThat(byId.get(old.getId()).getSchemaVersion()).isEqualTo("2.0.0");
        assertThat(byId.get(old.getId()).getPayload())
                .containsEntry("displayName", "Old")
                .containsEntry("tier", "STANDARD");
        assertThat(byId.get(current.getId())).isSameAs(current);
        assertThat(result.graph().getContents().getEdges()).isEqualTo(graph.getContents().getEdges());
        assertThat(result.upgraded().get(old.getId()).getStepsApplied()).containsExactly("1.0.0->2.0.0");
        assertThat(old.getSchemaVersion()).isEqualTo("1.0.0");   // input untouched
    }

    @Test
    void shouldValidateUpgradedPayload_againstLatestSchema() {
        UpgradeFixture fx = UpgradeFixture.products(new Product_1_0_0_to_2_0_0());
        ResolvedGraph graph = TestGraphs.graph(
                List.of(TestGraphs.entity("Product", "1.0.0", Map.of("name", "Widget"))), List.of());

        ResolvedGraph latest = fx.upgrader(GraphUpgrader.FailurePolicy.FAIL).upgrade(graph);

        assertThat(fx.validator.validateEntities(latest.getContents().getEntities()).isValid()).isTrue();
    }

    @Test
    void shouldThrow_whenRenameHasNoStepAndPolicyIsFail() {
        UpgradeFixture fx = UpgradeFixture.products();   // no steps -> additive fallback fails on rename
        ResolvedGraph graph = TestGraphs.graph(
                List.of(TestGraphs.entity("Product", "1.0.0", Map.of("name", "Widget"))), List.of());

        assertThatThrownBy(() -> fx.upgrader(GraphUpgrader.FailurePolicy.FAIL).upgrade(graph))
                .isInstanceOf(GraphUpgradeException.class);
    }

    @Test
    void shouldKeepStoredAndReport_whenPolicyIsKeepStored() {
        UpgradeFixture fx = UpgradeFixture.products();
        Entity old = TestGraphs.entity("Product", "1.0.0", Map.of("name", "Widget"));

        GraphUpgrader.Result result = fx.upgrader(GraphUpgrader.FailurePolicy.KEEP_STORED)
                .upgradeWithReport(TestGraphs.graph(List.of(old), List.of()));

        assertThat(result.graph().getContents().getEntities()).containsExactly(old);
        assertThat(result.failed()).containsKey(old.getId());
        assertThat(result.failed().get(old.getId()).getFailure()).isNotBlank();
    }
}
```

### Service over a mocked store

Mock only the port that *returns* the graph. `NamedGraphStore` is a final Kotlin class — Mockito 5
(inline mock maker by default) handles it.

```java
class LatestGraphServiceTest {
    private final UpgradeFixture fx = UpgradeFixture.products(new Product_1_0_0_to_2_0_0());
    private final NamedGraphStore store = mock(NamedGraphStore.class);
    private final LatestGraphService service =
            new LatestGraphService(store, fx.upgrader(GraphUpgrader.FailurePolicy.FAIL), fx.targets);

    @Test
    void shouldReturnLatest_whenStoredGraphHasOldPins() {
        Entity old = TestGraphs.entity("Product", "1.0.0", Map.of("name", "Widget"));
        ResolvedGraph stored = TestGraphs.graph(List.of(old), List.of());
        when(store.get(stored.getId())).thenReturn(stored);

        ResolvedGraph wire = service.get(stored.getId());

        assertThat(wire.getContents().getEntities()).singleElement()
                .satisfies(e -> assertThat(e.getSchemaVersion()).isEqualTo("2.0.0"));
        verify(store, never()).mutate(any(), any());   // no write-back on read
    }

    @Test
    void shouldRejectWrite_whenPinIsNotLatest() {
        GraphMutation mutation = new GraphMutation(
                new EntityMutation(new ArrayList<>(List.of(TestGraphs.entity("Product", "1.0.0", Map.of("name", "X")))),
                        new ArrayList<>()),
                new EdgeMutation(),
                MutationMode.MERGE);

        assertThatThrownBy(() -> service.mutate(UUID.randomUUID(), mutation))
                .isInstanceOf(GraphException.class);
        verifyNoInteractions(store);
    }
}
```

### Scenarios worth covering

| Scenario | Setup | Expect |
|----------|-------|--------|
| Exact latest pin | entity at `2.0.0` | returned as-is |
| Chain | steps `1.0.0→1.1.0`, `1.1.0→2.0.0` | `stepsApplied` has both, in order |
| Ambiguous | two steps `1.0.0→2.0.0` | `failure` mentions ambiguous path → `GraphUpgradeException` |
| Additive bump, no step | `1.1.0` adds optional field | upgraded, `getAdditiveFallback() == true` |
| Rename, no step | `2.0.0` renames field | `GraphUpgradeException` / `failed` entry |
| Unknown type | no target | returned as-is |

---

## 3. Seed-backed upgrade rig

Same seed files the app deploys (`objs.seeds.resources`), loaded in memory. Historic sample payloads
live as `kind: Graph` fixture seeds under `src/test/resources/upgrade-fixtures/`. `Graph` documents
are **parsed only** (`GraphSeedHandler.parse` builds entities without touching the store).

```yaml
# src/test/resources/upgrade-fixtures/product-1.0.0.yaml
apiVersion: objs.poc.org/v1
kind: Graph
name: fixture-product-1.0.0
entities:
  - key: widget
    type: Product
    schemaVersion: "1.0.0"
    payload:
      name: Widget
```

```java
final class SeedUpgradeRig {
    final UpgradeFixture fx;
    final List<ResolvedGraph> graphs;

    private SeedUpgradeRig(UpgradeFixture fx, List<ResolvedGraph> graphs) {
        this.fx = fx;
        this.graphs = graphs;
    }

    static SeedUpgradeRig load(List<String> ontology, List<String> graphSeeds,
                               List<SchemaUpgradeStep> steps, Map<String, Class<?>> latestClasses) {
        UpgradeFixture fx = new UpgradeFixture(ontology.stream().map(SeedUpgradeRig::read).toList(), steps, latestClasses);

        GraphSeedHandler parser = new GraphSeedHandler(mock(NamedGraphStore.class));   // parse() never uses the store
        List<ResolvedGraph> graphs = graphSeeds.stream()
                .flatMap(path -> SeedYaml.INSTANCE.parseDocuments(read(path)).stream())
                .filter(doc -> SeedModelsKt.SEED_KIND_GRAPH.equals(doc.getKind()))
                .map(doc -> {
                    SeedGraphPayload p = (SeedGraphPayload) parser.parse(doc).getPayload();
                    return new ResolvedGraph(p.getGraphId(), p.getAnnotations(),
                            new GraphContents(p.getGraph().getEntities(), p.getGraph().getEdges()));
                })
                .toList();
        return new SeedUpgradeRig(fx, graphs);
    }

    List<Entity> entities() {
        return graphs.stream().flatMap(g -> g.getContents().getEntities().stream()).toList();
    }

    private static String read(String classpath) {
        try (InputStream in = SeedUpgradeRig.class.getClassLoader().getResourceAsStream(classpath)) {
            return new String(Objects.requireNonNull(in, "Missing " + classpath).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
```

```java
class SeedUpgradeTest {
    private final SchemaEvolutionConfiguration prod = new SchemaEvolutionConfiguration();
    private final SeedUpgradeRig rig = SeedUpgradeRig.load(
            List.of("seeds/my-ontology.yaml"),
            List.of("seeds/my-demo-graph.yaml", "upgrade-fixtures/product-1.0.0.yaml"),
            List.of(new Product_1_0_0_to_2_0_0()),           // same step classes as production
            SchemaEvolutionConfiguration.LATEST_CLASSES);

    @Test
    void shouldPointProductionTargetsAtCatalogLatest() {
        SchemaUpgradeTargetRegistry prodTargets = prod.schemaUpgradeTargets();
        for (Schema latest : rig.fx.support.latestEntitySchemas()) {
            SchemaUpgradeTarget target = prodTargets.latest(latest.getType());
            if (target != null) {
                assertThat(target.getSchemaVersion()).as(latest.getType()).isEqualTo(latest.getVersion());
            }
        }
    }

    @TestFactory
    Stream<DynamicTest> shouldHaveFixtureForEveryHistoricVersion() {
        return rig.fx.schemas.all().stream()
                .filter(s -> s.getUsage() == SchemaUsage.ENTITY)
                .filter(s -> !s.getVersion().equals(rig.fx.support.latestEntitySchema(s.getType()).getVersion()))
                .map(s -> DynamicTest.dynamicTest(s.getType() + "@" + s.getVersion() + " has fixture", () ->
                        assertThat(rig.entities()).anyMatch(e ->
                                e.getType().equals(s.getType()) && e.getSchemaVersion().equals(s.getVersion()))));
    }

    @TestFactory
    Stream<DynamicTest> shouldValidateStoredPayloads_againstPinnedSchema() {
        return rig.entities().stream().map(e -> DynamicTest.dynamicTest(
                "stored " + e.getType() + "@" + e.getSchemaVersion() + " " + e.getId(),
                () -> assertThat(rig.fx.validator.validateEntities(List.of(e)).getIssues()).isEmpty()));
    }

    @TestFactory
    Stream<DynamicTest> shouldUpgradeEveryGraph_andValidateAgainstLatest() {
        GraphUpgrader upgrader = rig.fx.upgrader(GraphUpgrader.FailurePolicy.KEEP_STORED);
        return rig.graphs.stream().map(g -> DynamicTest.dynamicTest("graph " + g.getId(), () -> {
            GraphUpgrader.Result result = upgrader.upgradeWithReport(g);
            assertThat(result.failed()).as("upgrade failures").isEmpty();
            assertThat(rig.fx.validator.validateEntities(result.graph().getContents().getEntities()).getIssues())
                    .as("latest-schema validation").isEmpty();
        }));
    }
}
```

What breaks the build:

- new schema version in seeds without production target update
- historic version without fixture payload
- seed graph not valid against its own pin
- missing / wrong step, or step output not valid against the **latest seed schema**

Optional golden files (`product-1.0.0.expect.json`) can assert exact upgraded payloads per entity
key, like SBOM `ComponentUpgradeRegressionTest`.

---

## 4. Spring wiring

`objs-autoconfigure` already provides `SchemaCatalog`, `AllowedEdgeCatalog`, `CatalogSupport`, and
`NamedGraphStore`. The app adds payload mapper, steps, targets, upgrader, and a facade.

### Configuration

```java
@Configuration
public class SchemaEvolutionConfiguration {

    /** Lane A latest classes per type. Versions come from here; startup check compares with catalog. */
    static final Map<String, Class<?>> LATEST_CLASSES = Map.of("Product", ProductV2.class);
    static final Map<String, SchemaUpgradeTarget> LATEST_TARGETS = Map.of(
            "Product", new SchemaUpgradeTarget("2.0.0", ProductV2.class));

    @Bean
    public PayloadMapper upgradePayloadMapper() {
        return new PayloadMapper(
                JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build());
    }

    @Bean
    public SchemaUpgradeRegistry schemaUpgradeRegistry(ObjectProvider<SchemaUpgradeStep> steps) {
        return InMemorySchemaUpgradeRegistry.of(steps.orderedStream().toList());
    }

    @Bean
    public SchemaUpgradeTargetRegistry schemaUpgradeTargets() {
        return LATEST_TARGETS::get;
    }

    @Bean
    public GraphUpgrader graphUpgrader(PayloadMapper upgradePayloadMapper,
                                       SchemaUpgradeRegistry schemaUpgradeRegistry,
                                       SchemaUpgradeTargetRegistry schemaUpgradeTargets) {
        return new GraphUpgrader(upgradePayloadMapper, schemaUpgradeRegistry, schemaUpgradeTargets);
    }
}
```

Steps are ordinary beans, so adding one is a single class:

```java
@Component
public final class Product_1_0_0_to_2_0_0 extends ClassToClassUpgradeStep<ProductV1, ProductV2> { /* as above */ }
```

`ObjectProvider` keeps startup working when no steps exist yet (plain `List<SchemaUpgradeStep>`
injection fails on zero beans). If the app already exposes another `PayloadMapper` bean, qualify
injection by bean name.

### Startup consistency check

Seeds load at startup and catalogs hydrate before the app is ready; check after
`ApplicationReadyEvent` so a forgotten target bump fails fast.

```java
@Component
public class SchemaUpgradeTargetsCheck {
    private final CatalogSupport catalog;
    private final SchemaUpgradeTargetRegistry targets;

    public SchemaUpgradeTargetsCheck(CatalogSupport catalog, SchemaUpgradeTargetRegistry targets) {
        this.catalog = catalog;
        this.targets = targets;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verify() {
        List<String> mismatches = new ArrayList<>();
        for (Schema latest : catalog.latestEntitySchemas()) {
            SchemaUpgradeTarget target = targets.latest(latest.getType());
            if (target != null && !target.getSchemaVersion().equals(latest.getVersion())) {
                mismatches.add("%s: target %s, catalog latest %s".formatted(
                        latest.getType(), target.getSchemaVersion(), latest.getVersion()));
            }
        }
        if (!mismatches.isEmpty()) {
            throw new IllegalStateException("Schema upgrade targets out of date: " + mismatches);
        }
    }
}
```

### Facade (read upgrades, write accepts latest only)

```java
@Service
public class LatestGraphService {
    private final NamedGraphStore store;
    private final GraphUpgrader upgrader;
    private final SchemaUpgradeTargetRegistry targets;

    public LatestGraphService(NamedGraphStore store, GraphUpgrader upgrader, SchemaUpgradeTargetRegistry targets) {
        this.store = store;
        this.upgrader = upgrader;
        this.targets = targets;
    }

    public ResolvedGraph get(UUID id) {
        ResolvedGraph stored = store.get(id);
        if (stored == null) {
            throw new GraphException("GRAPH_NOT_FOUND", "Graph " + id + " not found");
        }
        return upgrader.upgrade(stored);
    }

    public ValidationResult mutate(UUID graphId, GraphMutation mutation) {
        for (Entity e : mutation.getEntities().getSet()) {
            SchemaUpgradeTarget target = targets.latest(e.getType());
            if (target != null && !target.getSchemaVersion().equals(e.getSchemaVersion())) {
                throw new GraphException("SCHEMA_VERSION_NOT_LATEST",
                        "%s must be written at %s, got %s".formatted(e.getType(), target.getSchemaVersion(), e.getSchemaVersion()));
            }
        }
        return store.mutate(graphId, mutation);
    }
}
```

Write rules:

- Clients send latest payloads only; persist at latest pin (validator checks against latest schema).
- An entity stored at an old pin becomes latest when the client writes it back — explicit write,
  not silent rewrite.
- In `MERGE`, put only entities the client actually changed into `set`; never bump untouched ones.

### Controller + error mapping

```java
@RestController
@RequestMapping("/api/v1/my-app/graphs")
public class LatestGraphController {
    private final LatestGraphService service;

    public LatestGraphController(LatestGraphService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResolvedGraph get(@PathVariable UUID id) {
        return service.get(id);
    }
}

@RestControllerAdvice
class SchemaUpgradeErrors {
    @ExceptionHandler(GraphUpgradeException.class)
    ProblemDetail upgradeFailed(GraphUpgradeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        pd.setProperty("graphId", ex.getGraphId());
        pd.setProperty("entityId", ex.getEntity().getId());
        pd.setProperty("storedSchemaVersion", ex.getEntity().getSchemaVersion());
        return pd;
    }
}
```

Prefer an app DTO over exposing `ResolvedGraph` directly once the wire contract stabilises; the
upgrade step stays the same.

### Optional

- **Chain cache:** wrap `SchemaUpgradeRegistry` and memoise `findChain` per `(type, from, to)` for
  large graphs.
- **Stored pin for support:** add `storedSchemaVersion` to the DTO or a debug-only
  `?representation=saved` route; not the default.
- **Explicit migration:** an operator job (load old pins → `GraphUpgrader` → `store.mutate`) shortens
  chains; keep it separate from reads.
