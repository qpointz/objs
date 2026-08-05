# objs-core (Kotlin)

**Module:** `:objs-core`  
**Packages:** `org.poc.objs.core.*`

## Role

Entity SDK + validation + JPA/Flyway persistence for the entity store.

## Package map

| Package | Types | Responsibility |
|---------|-------|----------------|
| `org.poc.objs.core` | `ObjsCore` | Module marker |
| `…domain` | `BoMEntity`, `BoMEdge`, `BoMGraph`, `BoMSubgraph`, `BoMSchema*`, `BoAllowedEdge*` | In-memory domain + catalogs |
| `…match` | `BoMMatcher`, `BoMSourceCapableMatcher`, `BoMCandidateSource`, `MatchAllAnnotationMatcher`, `BoMMatchExpression` | Annotation matching + candidate sources |
| `…subgraph` | `BoMSubgraphSelector` | Induced subgraph selection |
| `…validation` | `BoMValidator`, `BoMPersistGate`, `BoMValidationResult` | Schema + allow-list; two-stage persist gate |
| `…persistence` | `BoMEntityRecord`, `BoMEdgeRecord`, repos, `BoMGraphStore`, autoconfig | JPA + Flyway + store facade |

## Key behaviours

- **SDK:** construct any `BoMGraph` in memory without validation.
- **Schemas:** PostgreSQL-authoritative `BoMSchemaCatalog` keyed by `(type, version)`.
- **Allow-list:** `BoMAllowedEdgeCatalog` with properties policy `NONE` | `SCHEMA`.
- **Persist gate:** stage 1 entities vs schema → assign missing `UUID.randomUUID()` → stage 2 edges vs payload∪store.
- **Id rule:** no id → create (`UUID.randomUUID()`); id not in store → create with client id; id in store → update.
- **DB:** Flyway graph tables use the `bom_graph_*` prefix; tests run on H2 and Testcontainers PostgreSQL.

## Tests

- Domain / subgraph / validator / persist-gate unit tests (no Spring)
- `BoMGraphStoreTest` — `@DataJpaTest` + Flyway + H2 round-trip and batch validation

See also [`../graph/`](../graph/README.md).
