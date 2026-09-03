# WI-002 — Api foundation move + store ports

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 2 — Api foundation  
**Status:** complete  
**Depends on:** WI-001  
**Examples:** **—** (gremlin recompile; SBOM compile may follow in WI-005)

## Goal

Move non-persistence model packages into `:objs-api` per G-A20, add store **ports**, and make `:objs-gremlin-core` depend on `:objs-api` only.

## Scope

- [x] Move per G-A20 boundary (schemas/InMemory catalogs, matcher contract + in-memory/JEXL, validation contracts, seed parse/SPI, versioning strategy, identity/merge/export helpers as applicable)
- [x] Keep in core: JPA records/repos→soon DAOs, store impls, SQL pushdown, seed apply/ledger, networknt `Validator` impl
- [x] Store ports on api; wire gremlin to ports
- [x] Add **JEXL** to `:objs-api` (G-A21); do **not** add networknt/Jackson 2 to api
- [x] Fix imports across foundation modules; keep behaviour unchanged
- [x] Move matching unit tests to `:objs-api` where packages moved

## Out of scope

- DAO / Spring strip (WI-003)
- Autoconfigure module (WI-004)
- Export rename (WI-007)

## Acceptance

- [x] `:objs-gremlin-core` `api` depends on `:objs-api` only (no `:objs-core`)
- [x] `:objs-api:test` covers moved model/matcher tests
- [x] `./gradlew :objs-api:test :objs-gremlin-core:compileKotlin :objs-core:compileKotlin` green
