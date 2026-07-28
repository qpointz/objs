# objs-core (Kotlin)

**Module:** `:objs-core`  
**Packages:** `org.poc.objs.core.*`

## Role

Entity SDK + validation + JPA/Flyway persistence for the entity store.

## Package map

| Package | Types | Responsibility |
|---------|-------|----------------|
| `org.poc.objs.core` | `ObjsCore` | Module marker |
| `…domain` | `BoEntity`, `BoEdge`, `BoGraph`, `BoSubgraph`, `BoSchema*`, `BoAllowedEdge*` | In-memory domain + catalogs |
| `…match` | `BoAnnotationMatcher`, `MatchAllAnnotationMatcher` | Annotation matching strategies |
| `…subgraph` | `BoSubgraphSelector` | Induced subgraph selection |
| `…validation` | `BoValidator`, `BoPersistGate`, `BoValidationResult` | Schema + allow-list; two-stage persist gate |
| `…persistence` | `BoEntityRecord`, `BoEdgeRecord`, repos, `BoGraphStore`, autoconfig | JPA + Flyway + store facade |

## Key behaviours

- **SDK:** construct any `BoGraph` in memory without validation.
- **Schemas:** in-memory `BoSchemaCatalog` keyed by `(type, version)`.
- **Allow-list:** `BoAllowedEdgeCatalog` with properties policy `NONE` | `SCHEMA`.
- **Persist gate:** stage 1 entities vs schema → assign missing `UUID.randomUUID()` → stage 2 edges vs payload∪store.
- **Id rule:** no id → create (`UUID.randomUUID()`); id not in store → create with client id; id in store → update.
- **DB:** Flyway `V1__bo_entity_edge.sql`; tests on H2 (`MODE=PostgreSQL`); runtime PostgreSQL.

## Tests

- Domain / subgraph / validator / persist-gate unit tests (no Spring)
- `BoGraphStoreTest` — `@DataJpaTest` + Flyway + H2 round-trip and batch validation

See also [`../graph/`](../graph/README.md).
