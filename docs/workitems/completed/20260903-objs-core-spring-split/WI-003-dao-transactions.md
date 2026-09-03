# WI-003 — DAO layer and transactions

**Story:** [`STORY.md`](STORY.md)  
**Stage:** 3 — DAO + TX  
**Status:** done — [`56e3997`](https://gitlab.qpointz.io/sandbox/bom-poc/-/commit/56e3997)  
**Depends on:** WI-002  
**Examples:** **—**

## Goal

Make `:objs-core` Spring-free at the persistence boundary: replace Spring Data repositories with `EntityManager` DAOs and route all store/seed/catalog transactions through an **internal** UoW — hidden from public API (G-A2/A3).

## Scope

- [x] Delete `JpaRepository` interfaces; add DAO classes mirroring current query methods (G-A1)
- [x] Internal UoW SPI + default EM-backed impl (join-if-active); Spring `TransactionTemplate` lands in WI-004
- [x] Remove `@Transactional`, `@Service`, `@Component` from objs-core stores, catalogs, seed pipeline
- [x] Remove Spring `@ConfigurationProperties` from objs-core (plain settings types if needed)
- [x] objs-core `build.gradle.kts`: Jakarta JPA + Hibernate + Flyway core only — **no** Spring artifacts
- [x] Move Boot autoconfig classes out of objs-core (`:objs-autoconfigure` scaffold; Spring UoW remains WI-004)

Minimal `:objs-autoconfigure` created so Boot apps keep compiling. Persistence tests still `@DataJpaTest` + that autoconfig (temporary harness); DAO method names retargeted so tests compile. Full harness is WI-006.

## Out of scope

- Full `:objs-autoconfigure` (WI-004) — Spring `TransactionTemplate` UoW adapter, `@EnableTransactionManagement`, expanded tests
- Remaining consumer Gradle (WI-005)
- Full test migration (WI-006) — keep tests green with minimal harness updates here

## Acceptance

- `./gradlew :objs-core:dependencies --configuration compileClasspath` shows no `org.springframework`
- Domain/matcher/validation tests still pass
- Persistence code compiles against DAOs (tests may use temporary harness until WI-006)
