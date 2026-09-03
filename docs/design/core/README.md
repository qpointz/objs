# objs-api / objs-persistence / objs-autoconfigure

**Modules:** `:objs-api` · `:objs-persistence` · `:objs-autoconfigure` (Boot adapter)  
**Story:** [C-25 objs-core-spring-split](../../workitems/in-progress/objs-core-spring-split/STORY.md) · [GAPS](../../workitems/in-progress/objs-core-spring-split/GAPS.md) · [spring-split.md](spring-split.md) · [persistence-backends.md](persistence-backends.md)

## Role

| Module | Owns |
|--------|------|
| `:objs-api` | Foundational model: graph primitives, schemas/InMemory catalogs, matcher contract + in-memory/JEXL, validation **contracts**, seed **parse**, store **ports** |
| `:objs-persistence` | Persistence: JPA/DAOs, store **impls**, SQL pushdown, Flyway SQL, seed **apply**/ledger, networknt `Validator` **impl** — **no Spring** |
| `:objs-autoconfigure` | Tiny Boot wiring: `DataSource`/`EntityManager` → beans, `@ConfigurationProperties`, Spring UoW, seed startup |

Gradle module is `:objs-persistence` (G-X7). Java packages remain `org.poc.objs.core.*` until a later tidy.

## Package map

| Location | Responsibility |
|----------|----------------|
| `org.poc.objs.api.*` | Model + ports |
| `org.poc.objs.core.persistence.*` | JPA records, DAOs, stores, Flyway helpers |
| `org.poc.objs.core.*` (shrinking) | Persistence-only leftovers until package tidy |
| `org.poc.objs.autoconfigure.*` | Boot autoconfig |

## Key behaviours

- **SDK / model:** construct graphs, match in memory, hold catalogs — prefer `:objs-api`.
- **Persist:** DAOs + internal UoW; Boot apps never open TX themselves — write-path sketch [`../graph/persist-sketch.md`](../graph/persist-sketch.md).
- **DB:** objs Flyway (`flyway_schema_history_objs`, vendor SQL in persistence JAR) before Boot Flyway — see [`../graph/persistence.md`](../graph/persistence.md).
- **Other backends / further splits:** deferred strategies — see [`persistence-backends.md`](persistence-backends.md).

## Tests

- Model / matcher unit tests → `:objs-api`
- Persistence harness (EMF + objs Flyway + EM UoW) → `:objs-persistence`
- Boot slices → `:objs-autoconfigure`

See also [`../graph/`](../graph/README.md).
