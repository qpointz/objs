# objs-core (Kotlin)

**Module:** `:objs-core`  
**Packages:** `org.poc.objs.core.*`

## Role

Entity SDK + validation + JPA/Flyway persistence for the entity store.

## Package map

| Package | Types | Responsibility |
|---------|-------|----------------|
| `org.poc.objs.core` | `ObjsCore` | Module marker |
| `…domain` | `BoMEntity`, `BoMEdge`, `BoMGraph`, `BoMGraphContents`, `BoMSchema*`, `BoAllowedEdge*` | In-memory domain + catalogs |
| `…match` | `BoMMatcher`, `BoMSourceCapableMatcher`, `BoMObjExprMatcher`, `BoMGraphExprMatcher`, `BoMAllGraphsMatcher`, `BoMChainedMatcher` | Matcher DSL + candidate sources |
| `…validation` | `BoMValidator`, `BoMPersistGate`, `BoMValidationResult` | Schema + allow-list; two-stage persist gate |
| `…persistence` | `BoMEntityRecord`, `BoMEdgeRecord`, `BoMGraphRecord`, repos, `BoMGraphStore`, `BoMNamedGraphStore`, `ObjsFlywayAutoConfiguration` | JPA + two-line Flyway + store facade |

## Key behaviours

- **SDK:** construct any `BoMGraph` in memory without validation.
- **Schemas:** PostgreSQL-authoritative `BoMSchemaCatalog` keyed by `(type, version)`.
- **Allow-list:** `BoMAllowedEdgeCatalog` with properties policy `NONE` | `SCHEMA`.
- **Persist gate:** stage 1 entities vs schema → assign missing `UUID.randomUUID()` → stage 2 edges vs payload∪store.
- **Id rule:** no id → create (`UUID.randomUUID()`); id not in store → create with client id; id in store → update.
- **DB:** objs-core applies `bom_*` via its own Flyway (`flyway_schema_history_objs`, vendor SQL in
  the JAR). Derived apps depend on `objs-core` and keep an independent Boot Flyway `V1` for app
  tables — see [`../graph/persistence.md`](../graph/persistence.md) and
  [`docs/workitems/RULES.md`](../../workitems/RULES.md) **Flyway (library + derived apps)**. Tests run
  on H2 and Testcontainers PostgreSQL.

## Tests

- Domain / subgraph / validator / persist-gate unit tests (no Spring)
- `BoMGraphStoreTest` — `@DataJpaTest` + objs Flyway + H2 round-trip and batch validation
- `ObjsFlywayVendorTest` / `ObjsFlywayAutoConfigurationTest` — `{vendor}` from JDBC URL; Boot Flyway off

See also [`../graph/`](../graph/README.md).
